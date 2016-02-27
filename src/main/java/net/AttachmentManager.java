package net;

import model.AggregateTableInfo;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.wink.client.WinkClient;
import utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

//!!!ATTENTION!!! One AttachmentManager per table
public class AttachmentManager {
  private class DownloadProducer implements Runnable {
    private BlockingQueue<String[]> q;
    private String rowId;
    private Set<String> files;

    DownloadProducer(BlockingQueue<String[]> queue, String rowId, Set<String> files) {
      this.q = queue;
      this.rowId = rowId;
      this.files = files;
    }

    @Override
    public void run() {
      try {
        for (String filename : files) {
          q.put(new String[] { rowId, filename });
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private class DownloadTask implements Runnable {
    private BlockingQueue<String[]> q;

    DownloadTask(BlockingQueue<String[]> q) {
      this.q = q;
    }

    @Override
    public void run() {
      try {
/*                while (q.isEmpty()) {
                    //wait until DownloadProducer enqueues a file
                    Thread.sleep(500);
                }
                while (!q.isEmpty()) {
                    String[] file = q.take();
                    downloadFile(file[0], file[1]);
                }*/

        do {
          String[] file = q.take();
          downloadFile(file[0], file[1]);
        } while (!q.isEmpty());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static final int DOWNLOAD_THREADS = 20;

  private AggregateTableInfo table;
  private Map<String, Map<String, String>> allAttachments;
  //some rows lack attachment manifest, this map keeps track of that
  private Map<String, Boolean> hasManifestMap;
  private WinkClient wc;
  private BlockingQueue<String[]> bq;

  public AttachmentManager(AggregateTableInfo table, WinkClient wc) {
    if (table.getSchemaETag() == null) {
      throw new IllegalStateException("SchemaETag has not been set!");
    }

    this.table = table;
    this.wc = wc;
    this.allAttachments = new ConcurrentHashMap<String, Map<String, String>>();
    this.hasManifestMap = new ConcurrentHashMap<String, Boolean>();

    this.bq = new LinkedBlockingQueue<String[]>(DOWNLOAD_THREADS * 3);
    for (int i = 0; i < DOWNLOAD_THREADS; i++) {
      new Thread(new DownloadTask(bq)).start();
    }
    //TODO: implement mechanism to shutdown unused threads
  }

  public void getListOfRowAttachments(String rowId) throws Exception {
    //TODO: download manifest for multiple rows in parallel

    if (!this.allAttachments.containsKey(rowId)) {
      Map<String, String> attachmentsMap = new ConcurrentHashMap<String, String>();
      this.hasManifestMap.put(rowId, true);

      try {
        JSONArray attachments = wc
            .getManifestForRow(this.table.getServerUrl(), this.table.getAppId(),
                this.table.getTableId(), this.table.getSchemaETag(), rowId).getJSONArray("files");

        if (attachments.size() < 1) {
          this.hasManifestMap.put(rowId, false);
        } else {
          for (int i = 0; i < attachments.size(); i++) {
            JSONObject attachmentJson = attachments.getJSONObject(i);
            attachmentsMap.put(attachmentJson.optString("filename"), attachmentJson.optString("downloadUrl"));
          }
        }
      } catch (Exception e) {
        System.out.println("Attachments Manifest Missing!");
        this.hasManifestMap.put(rowId, false);
      }

      this.allAttachments.put(rowId, attachmentsMap);
    }
  }

  public URL getAttachmentUrl(String rowId, String filename, boolean localUrl) throws IOException {
    if (!this.allAttachments.containsKey(rowId)) {
      throw new IllegalStateException("Row manifest has not been downloaded");
    }

    if (!this.hasManifestMap.get(rowId)) {
      return null;
    }

    if (!this.allAttachments.get(rowId).containsKey(filename)) {
      System.out.println(filename);
      throw new IllegalArgumentException("Filename not found");
    }

    if (localUrl) {
      return new URL("file:///" + getAttachmentLocalPath(rowId, filename));
    } else {
      return new URL(this.allAttachments.get(rowId).get(filename));
    }
  }

  public void downloadAttachments(String rowId, boolean scanRawJsonOnly) throws IOException {
    if (!this.allAttachments.containsKey(rowId)) {
      throw new IllegalStateException("Row manifest has not been downloaded");
    }

    if (this.hasManifestMap.get(rowId)) {
      if (scanRawJsonOnly) {
        downloadFile(rowId, getJsonFilename(rowId));
      } else {
        try {
          Thread t = new Thread(
              new DownloadProducer(this.bq, rowId, this.allAttachments.get(rowId).keySet()));
          t.start();
          t.join(); //wait for all files to be enqueued
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public InputStream getScanRawJsonStream(String rowId) throws IOException {
    //TODO: make sure file has been downloaded

    if (!this.hasManifestMap.get(rowId)) {
      return null;
    }

    return Files.newInputStream(getAttachmentLocalPath(rowId, getJsonFilename(rowId)));
  }

  public void waitForAttachmentDownload() throws InterruptedException {
    while (!this.bq.isEmpty()) {
      Thread.sleep(500);
    }
  }

  private Path getAttachmentLocalPath(String rowId, String filename) throws IOException {
    //Warning: Doesn't check if filename is valid

    String sanitizedRowId = WinkClient.convertRowIdForInstances(rowId);

    if (Files.notExists(Paths.get(FileUtils.getInstancesPath(table).toString(), sanitizedRowId))) {
      Files.createDirectories(
          Paths.get(FileUtils.getInstancesPath(table).toString(), sanitizedRowId));
    }

    return Paths.get(FileUtils.getInstancesPath(this.table).toString(), sanitizedRowId, filename)
        .toAbsolutePath();
  }

  private String getJsonFilename(String rowId) {
    return "raw_" + WinkClient.convertRowIdForInstances(rowId) + ".json";
  }

  private void downloadFile(String rowId, String filename) throws IOException {
    //        System.out.println("Downloading " + rowId + "'s" + filename);

    Path savePath = getAttachmentLocalPath(rowId, filename);

    if (Files.notExists(savePath)) {
      try {
        InputStream in = getAttachmentUrl(rowId, filename, false).openStream();
        Files.copy(in, savePath);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
