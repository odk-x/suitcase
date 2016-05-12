package net;

import model.AggregateInfo;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;
import org.opendatakit.wink.client.WinkClient;
import utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages downloading attachments and information about attachments
 *
 * !!!ATTENTION!!! One AttachmentManager per table
 */
public class AttachmentManager {
  private AggregateInfo table;
  private Map<String, JSONObject> attachmentManifests;
  private Map<String, Map<String, String>> allAttachments;
  private WinkClient wc;
  private String savePath;
  
  public AttachmentManager(AggregateInfo table, WinkClient wc, String savePath) {
    this.table = table;
    this.wc = wc;
    this.savePath = savePath;

    this.attachmentManifests = new HashMap<>();
    this.allAttachments = new HashMap<>();
  }

  /**
   * Retrieves attachment manifest for a row.
   * After processing manifest, info is stored in allAttachments and hasManifestMap.
   *
   * @param rowId
   */
  public void getListOfRowAttachments(String rowId) {
    if (!this.allAttachments.containsKey(rowId)) {
      try {
        JSONObject manifest =
            wc.getManifestForRow(this.table.getServerUrl(), this.table.getAppId(),
                this.table.getCurrentTableId(),
                this.table.getSchemaETag(this.table.getCurrentTableId()), rowId
            );
        JSONArray attachments = manifest.getJSONArray("files");

        if (attachments.size() > 0) {
          attachmentManifests.put(rowId, manifest);

          Map<String, String> attachmentsMap = new HashMap<>();
          for (int i = 0; i < attachments.size(); i++) {
            JSONObject attachmentJson = attachments.getJSONObject(i);
            attachmentsMap.put
                (attachmentJson.optString("filename"), attachmentJson.optString("downloadUrl"));
          }
          allAttachments.put(rowId, attachmentsMap);
        }
      } catch (Exception e) {
        System.out.println("Attachments Manifest Missing!");
      }
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

    if (!this.attachmentManifests.containsKey(rowId)) {
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

    if (this.attachmentManifests.containsKey(rowId)) {
      try {
        if (scanRawJsonOnly) {
          wc.getFileForRow(
              table.getServerUrl(), table.getAppId(), table.getCurrentTableId(),
              table.getSchemaETag(table.getCurrentTableId()), rowId, false,
              getAttachmentLocalPath(rowId, getJsonFilename(rowId)).toAbsolutePath().toString(),
              getJsonFilename(rowId)
          );
        } else {
          wc.batchGetFilesForRow(
              table.getServerUrl(), table.getAppId(), table.getCurrentTableId(),
              table.getSchemaETag(table.getCurrentTableId()),
              rowId, getAttachmentLocalDir(rowId).toString(), attachmentManifests.get(rowId), -1
          );
        }
      }
      catch (Exception e) {
        e.printStackTrace();
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
    if (!this.attachmentManifests.containsKey(rowId)) {
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

  /**
   * Infers local path to attachment directory with rowId and table info.
   *
   * @param rowId
   * @return
   */
  private Path getAttachmentLocalDir(String rowId) throws IOException {
    String sanitizedRowId = WinkClient.convertRowIdForInstances(rowId);
    String insPath = FileUtils.getInstancesPath(table, savePath).toString();

    if (Files.notExists(Paths.get(insPath, sanitizedRowId))) {
      Files.createDirectories(Paths.get(insPath, sanitizedRowId));
    }

    return Paths.get(insPath, sanitizedRowId).toAbsolutePath();
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
    return Paths.get(getAttachmentLocalDir(rowId).toString(), filename).toAbsolutePath();
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
}
