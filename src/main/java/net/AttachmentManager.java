package net;

import model.AggregateTableInfo;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.wink.client.WinkClient;
import utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages downloading attachments and information about attachments
 *
 * !!!ATTENTION!!! One AttachmentManager per table
 */
public class AttachmentManager {
  //Producer-Consumer pair with DownloadTask
  private class DownloadProducer implements Runnable {
    private final BlockingQueue<String[]> q;
    private final String rowId;
    private final Set<String> files;
    
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

  //Producer-Consumer pair with DownloadProducer
  private class DownloadTask implements Runnable {
    private final BlockingQueue<String[]> q;

    DownloadTask(BlockingQueue<String[]> q) {
      this.q = q;
    }

    @Override
    public void run() {
      try {
        do {
          String[] file = q.take(); //take blocks until q has at least 1 object
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
  private String userName;
  private String password;
  private String savePath;
  
  public AttachmentManager(
      AggregateTableInfo table, WinkClient wc, String userName, String password, String savePath) {
    if (table.getSchemaETag() == null) {
      throw new IllegalStateException("SchemaETag has not been set!");
    }

    this.table = table;
    this.wc = wc;
    this.userName = userName;
    this.password = password;
    this.savePath = savePath;
    this.allAttachments = new ConcurrentHashMap<>();
    this.hasManifestMap = new ConcurrentHashMap<>();

    this.bq = new LinkedBlockingQueue<>(DOWNLOAD_THREADS * 3);
    for (int i = 0; i < DOWNLOAD_THREADS; i++) {
      //start threads early, DownloadTask waits for queue to be populated
      new Thread(new DownloadTask(bq)).start();
    }

    //TODO: Quasar library is probably better
  }

  /**
   * Retrieves attachment manifest for a row.
   * After processing manifest, info is stored in allAttachments and hasManifestMap.
   *
   * @param rowId
   */
  public void getListOfRowAttachments(String rowId) {
    //TODO: download manifest for multiple rows in parallel

    if (!this.allAttachments.containsKey(rowId)) {
      Map<String, String> attachmentsMap = new ConcurrentHashMap<>();
      this.hasManifestMap.put(rowId, true); //assumes that row has manifest

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

  /**
   * Retrieves URL for attachment.
   * If a localUrl is requested, url is inferred from filename and table info
   * If a row lacks attachment manifest, null is returned.
   * When allAttachment lacks record of requested rowId, IllegalStateException will be thrown.
   * When allAttachment lacks record of requested filename, IllegalArgumentException will be thrown.
   *
   * @param rowId
   * @param filename
   * @param localUrl  True to return url to local file, aka "file:///" url
   * @return
   * @throws IOException
   */
  public URL getAttachmentUrl(String rowId, String filename, boolean localUrl) throws IOException {
    if (!this.allAttachments.containsKey(rowId)) {
      throw new IllegalStateException("Row manifest has not been downloaded: " + rowId);
    }

    if (!this.hasManifestMap.get(rowId)) {
      return null;
    }

    if (!this.allAttachments.get(rowId).containsKey(filename)) {
      System.out.println(filename + ": File missing or invalid filename");
      return null;
    }

    if (localUrl) {
      return new URL("file:///" + getAttachmentLocalPath(rowId, filename));
    } else {
      return new URL(this.allAttachments.get(rowId).get(filename));
    }
  }

  /**
   * Downloads all attachments of a row, or just Scan's raw JSON
   * When allAttachment lacks record of requested rowId, IllegalStateException will be thrown.
   *
   * @param rowId
   * @param scanRawJsonOnly True to download only Scan's raw JSON
   * @throws IOException
   */
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
              new DownloadProducer(this.bq, rowId, this.allAttachments.get(rowId).keySet())
          );
          t.start();
          //wait for all files to be enqueued,
          //so that attachment downloading doesn't lag too far behind
          t.join();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Gets InputStream of Scan's Raw JSON.
   * Returns null if row lacks attachment manifest.
   *
   * Warning: This method doesn't check whether JSON had been downloaded.
   *
   * @param rowId
   * @return
   * @throws IOException
   */
  public InputStream getScanRawJsonStream(String rowId) throws IOException {
    if (!this.hasManifestMap.get(rowId)) {
      //This InputStream will only be consumed by ScanJson
      //It is designed to handle null InputStreams
      return null;
    }

    try {
      return Files.newInputStream(getAttachmentLocalPath(rowId, getJsonFilename(rowId)));
    } catch (NoSuchFileException e) {
      return null;
    }
  }

  public void waitForAttachmentDownload() throws InterruptedException {
    while (!this.bq.isEmpty()) {
      Thread.sleep(500);
    }
  }

  /**
   * Infers local path to attachment with rowId, filename and table info.
   *
   * Warning: Doesn't check if filename is valid (THIS IS INTENDED)
   *
   * @param rowId
   * @param filename
   * @return
   * @throws IOException
   */
  private Path getAttachmentLocalPath(String rowId, String filename) throws IOException {
    String sanitizedRowId = WinkClient.convertRowIdForInstances(rowId);
    String insPath = FileUtils.getInstancesPath(table, savePath).toString();

    if (Files.notExists(Paths.get(insPath, sanitizedRowId))) {
      Files.createDirectories(Paths.get(insPath, sanitizedRowId));
    }

    return Paths.get(insPath, sanitizedRowId, filename).toAbsolutePath();
  }

  /**
   * Infers Scan raw JSON's filename
   *
   * @param rowId
   * @return
   */
  private String getJsonFilename(String rowId) {
    return "raw_" + WinkClient.convertRowIdForInstances(rowId) + ".json";
  }

  private void downloadFile(String rowId, String filename) throws IOException {
    Path savePath = getAttachmentLocalPath(rowId, filename);

    if (Files.notExists(savePath)) {
      try {
        class AttachmentAuthenticator extends Authenticator {
          public PasswordAuthentication getPasswordAuthentication () {
            return new PasswordAuthentication (userName, password.toCharArray());
          }
        }  
        AttachmentAuthenticator authenticator = new AttachmentAuthenticator();
        Authenticator.setDefault(authenticator);
        URL attachmentUrl = getAttachmentUrl(rowId, filename, false);
        if (attachmentUrl != null) {
          InputStream in = attachmentUrl.openStream();
          Files.copy(in, savePath);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
