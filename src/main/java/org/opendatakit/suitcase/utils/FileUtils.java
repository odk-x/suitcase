package org.opendatakit.suitcase.utils;

import org.opendatakit.suitcase.model.CloudEndpointInfo;
import org.opendatakit.suitcase.model.CsvConfig;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {
  // paths
  private static final String DEFAULT_SAVE_PATH = "Download";
  private static final String DEFAULT_UPLOAD_PATH = "Upload";
  private static final String DEFAULT_UPDATE_PATH = "Update";
  private static final String INSTANCES_PATH = "instances";
  private static final String ASSETS_PATH = "assets";
  private static final String TABLES_PATH = "tables";

  // csv names
  private static final String CSV_EXTENSION = ".csv";
  private static final String CSV_MODIFIER_SEPARATOR = "_";
  private static final String DATA_CSV_MODIFIER = "data";
  private static final String LINK_CSV_MODIFIER = "link";
  private static final String FORMATTED_CSV_MODIFIER = "formatted";
  private static final String UNFORMATTED_CSV_MODIFIER = "unformatted";
  private static final String EXTRA_DATA_CSV_MODIFIER = "extra";

  /**
   * Checks whether a table is downloaded
   *
   * @param cloudEndpointInfo cloud endpoint info
   * @param tableId Table id 
   * @param savePath path
   * @return true if downloaded, false otherwise
   */
  public static boolean isDownloaded(CloudEndpointInfo cloudEndpointInfo, String tableId, String savePath) {
    return Files.exists(getBasePath(cloudEndpointInfo, tableId, savePath));
  }

  /**
   * Checks whether a CSV of a table is downloaded
   *
   * @param cloudEndpointInfo cloud endpoint info
   * @param savePath path
   * @param tableId table id
   * @param config csv config
   * @return true if downloaded, false otherwise
   */
  public static boolean isDownloaded(CloudEndpointInfo cloudEndpointInfo, String tableId, CsvConfig config,
                                     String savePath) {
    return Files.exists(getCSVPath(cloudEndpointInfo, tableId, config, savePath));
  }

  /**
   * Finds the Path to a csv file using server info, table id, csv config and save path
   *
   * @param cloudEndpointInfo cloud endpoint info
   * @param tableId table id
   * @param config csv config
   * @param savePath path
   * @return absolute Path
   */
  public static Path getCSVPath(CloudEndpointInfo cloudEndpointInfo, String tableId, CsvConfig config,
                                String savePath) {
    return getBasePath(cloudEndpointInfo, tableId, savePath).resolve(getCSVName(config));
  }

  /**
   * Returns the default save path
   *
   * @return absolute Path
   */
  public static Path getDefaultSavePath() {
    return Paths.get(DEFAULT_SAVE_PATH).toAbsolutePath();
  }
  
  /**
   * Returns the default save path
   *
   * @return absolute Path
   */
  public static Path getUpdateSavePath() {
    return Paths.get(DEFAULT_UPDATE_PATH).toAbsolutePath();
  }

  /**
   * Returns the default upload path
   *
   * @return absolute Path
   */
  public static Path getDefaultUploadPath() {
    return Paths.get(DEFAULT_UPLOAD_PATH).toAbsolutePath();
  }
  
  /**
   * Returns the default update path
   *
   * @return absolute Path
   */
  public static Path getDefaultUpdatePath() {
    return Paths.get(DEFAULT_UPDATE_PATH).toAbsolutePath();
  }

  /**
   *
   * @param cloudEndpointInfo cloud endpoint info
   * @param tableId table id
   * @param savePath path
   * @return absolute Path
   */
  public static Path getBasePath(CloudEndpointInfo cloudEndpointInfo, String tableId, String savePath) {
    return Paths.get(savePath, cloudEndpointInfo.getAppId(), tableId).toAbsolutePath();
  }

  /**
   *
   * @param cloudEndpointInfo cloud endpoint info
   * @param tableId table id
   * @param savePath path
   * @return absolute Path
   */
  public static Path getInstancesPath(CloudEndpointInfo cloudEndpointInfo, String tableId, String savePath) {
    return getBasePath(cloudEndpointInfo, tableId, savePath).resolve(INSTANCES_PATH);
  }

  public static String getCSVName(CsvConfig config) {
    StringBuilder csvNameBuilder = new StringBuilder();

    csvNameBuilder
        .append(config.isDownloadAttachment() ? DATA_CSV_MODIFIER : LINK_CSV_MODIFIER);

    csvNameBuilder
        .append(CSV_MODIFIER_SEPARATOR)
        .append(config.isScanFormatting() ? FORMATTED_CSV_MODIFIER : UNFORMATTED_CSV_MODIFIER);

    if (config.isExtraMetadata()) {
      csvNameBuilder
          .append(CSV_MODIFIER_SEPARATOR)
          .append(EXTRA_DATA_CSV_MODIFIER);
    }

    return csvNameBuilder.append(CSV_EXTENSION).toString();
  }

  public static void createDirectory(CloudEndpointInfo cloudEndpointInfo, CsvConfig config, String tableId,
                                     String savePath) throws IOException {
    if (config.isDownloadAttachment() || config.isScanFormatting()) {
      FileUtils.createInstancesDirectory(cloudEndpointInfo, tableId, savePath);
    } else {
      FileUtils.createBaseDirectory(cloudEndpointInfo, tableId, savePath);
    }
  }

  public static void deleteCsv(CloudEndpointInfo cloudEndpointInfo, CsvConfig config, String tableId, String
      savePath) throws IOException {
    if (config.isDownloadAttachment() || config.isScanFormatting()) {
      deleteDirectory(getInstancesPath(cloudEndpointInfo, tableId, savePath));
    }

    Files.delete(getCSVPath(cloudEndpointInfo, tableId, config, savePath));
  }

  public static boolean checkUploadDir(String path) {
    return Files.exists(Paths.get(path, ASSETS_PATH)) || Files.exists(Paths.get(path, TABLES_PATH));
  }

  private static void deleteDirectory(Path directory) throws IOException {
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);

        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc == null) {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        } else {
          throw exc;
        }
      }
    });
  }

  private static void createBaseDirectory(CloudEndpointInfo cloudEndpointInfo, String tableId, String savePath)
      throws IOException {
    Path basePath = getBasePath(cloudEndpointInfo, tableId, savePath);

    if (Files.notExists(basePath)) {
      Files.createDirectories(basePath);
    }
  }

  private static void createInstancesDirectory(CloudEndpointInfo cloudEndpointInfo, String tableId, String savePath)
      throws IOException {
    Path insPath = getInstancesPath(cloudEndpointInfo, tableId, savePath);

    if (Files.notExists(insPath)) {
      Files.createDirectories(insPath);
    }
  }
}
