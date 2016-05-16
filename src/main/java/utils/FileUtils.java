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
   * @param table
   * @param savePath
   * @return
   */
  public static boolean isDownloaded(AggregateInfo table, String savePath) {
    return Files.exists(getBasePath(table, savePath));
  }

  /**
   * Checks whether a CSV of a table is downloaded
   *
   * @param table
   * @param savePath
   * @return
   */
  public static boolean isDownloaded(AggregateInfo table, CsvConfig config, String savePath) {
    return Files.exists(getCSVPath(table, config, savePath));
  }

  public static Path getCSVPath(AggregateInfo table, CsvConfig config, String savePath) {
    return Paths.get(
        getBasePath(table, savePath).toString(), getCSVName(config));
  }

  public static Path getDefaultSavePath() {
    return Paths.get(DEFAULT_SAVE_PATH);
  }

  public static Path getBasePath(AggregateInfo table, String savePath) {
    return Paths.get(savePath, table.getAppId(), table.getCurrentTableId());
  }

  public static Path getInstancesPath(AggregateInfo table, String savePath) {
    return Paths.get(getBasePath(table, savePath).toString(), INSTANCES_PATH);
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

  public static void createBaseDirectory(AggregateInfo table, String savePath)
      throws IOException {
    Path basePath = getBasePath(table, savePath);

    if (Files.notExists(basePath)) {
      Files.createDirectories(basePath);
    }
  }

  public static void createInstancesDirectory(AggregateInfo table, String savePath)
      throws IOException {
    Path insPath = getInstancesPath(table, savePath);

    if (Files.notExists(insPath)) {
      Files.createDirectories(insPath);
    }
  }

  public static void deleteTableData(AggregateInfo table, String savePath) throws IOException {
    Files.walkFileTree(getBasePath(table, savePath), new SimpleFileVisitor<Path>() {
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

  public static boolean checkUploadDir(String path) {
    return Files.exists(Paths.get(path, ASSETS_PATH)) || Files.exists(Paths.get(path, TABLES_PATH));
  }
}
