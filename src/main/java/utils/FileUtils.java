package utils;

import model.AggregateInfo;
import model.CsvConfig;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {
  // paths
  private static final String DEFAULT_SAVE_PATH = "Download";
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
   * @param aggInfo
   * @param savePath
   * @return
   */
  public static boolean isDownloaded(AggregateInfo aggInfo, String tableId, String savePath) {
    return Files.exists(getBasePath(aggInfo, tableId, savePath));
  }

  /**
   * Checks whether a CSV of a table is downloaded
   *
   * @param aggInfo
   * @param savePath
   * @return
   */
  public static boolean isDownloaded(AggregateInfo aggInfo, String tableId, CsvConfig config,
      String savePath) {
    return Files.exists(getCSVPath(aggInfo, tableId, config, savePath));
  }

  /**
   * Finds the Path to a csv file using server info, table id, csv config and save path
   *
   * @param aggInfo
   * @param config
   * @param savePath
   * @return absolute Path
   */
  public static Path getCSVPath(AggregateInfo aggInfo, String tableId, CsvConfig config,
      String savePath) {
    return getBasePath(aggInfo, tableId, savePath).resolve(getCSVName(config));
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
   *
   * @param aggInfo
   * @param tableId
   * @param savePath
   * @return absolute Path
   */
  public static Path getBasePath(AggregateInfo aggInfo, String tableId, String savePath) {
    return Paths.get(savePath, aggInfo.getAppId(), tableId).toAbsolutePath();
  }

  /**
   *
   * @param aggInfo
   * @param savePath
   * @return absolute Path
   */
  public static Path getInstancesPath(AggregateInfo aggInfo, String tableId, String savePath) {
    return getBasePath(aggInfo, tableId, savePath).resolve(INSTANCES_PATH);
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

  public static void createDirectory(AggregateInfo aggInfo, CsvConfig config, String tableId,
      String savePath) throws IOException {
    if (config.isDownloadAttachment() || config.isScanFormatting()) {
      FileUtils.createInstancesDirectory(aggInfo, tableId, savePath);
    } else {
      FileUtils.createBaseDirectory(aggInfo, tableId, savePath);
    }
  }

  public static void deleteCsv(AggregateInfo aggInfo, CsvConfig config, String tableId, String
      savePath) throws IOException {
    if (config.isDownloadAttachment() || config.isScanFormatting()) {
      deleteDirectory(getInstancesPath(aggInfo, tableId, savePath));
    }

    Files.delete(getCSVPath(aggInfo, tableId, config, savePath));
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

  private static void createBaseDirectory(AggregateInfo aggInfo, String tableId, String savePath)
      throws IOException {
    Path basePath = getBasePath(aggInfo, tableId, savePath);

    if (Files.notExists(basePath)) {
      Files.createDirectories(basePath);
    }
  }

  private static void createInstancesDirectory(AggregateInfo aggInfo, String tableId, String savePath)
      throws IOException {
    Path insPath = getInstancesPath(aggInfo, tableId, savePath);

    if (Files.notExists(insPath)) {
      Files.createDirectories(insPath);
    }
  }
}
