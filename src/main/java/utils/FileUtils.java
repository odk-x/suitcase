package utils;

import model.AggregateInfo;
import model.CsvConfig;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {
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
    return Paths.get("Download");
  }

  public static Path getBasePath(AggregateInfo table, String savePath) {
    return Paths.get(savePath, table.getAppId(), table.getCurrentTableId());
  }

  public static Path getInstancesPath(AggregateInfo table, String savePath) {
    return Paths.get(getBasePath(table, savePath).toString(), "instances");
  }

  public static String getCSVName(CsvConfig config) {
    return (config.isDownloadAttachment() ? "data" : "link") +
        (config.isScanFormatting() ? "_formatted" : "_unformatted") +
        (config.isExtraMetadata() ? "_extra" : "") + ".csv";
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
    return Files.exists(Paths.get(path, "assets")) || Files.exists(Paths.get(path, "tables"));
  }
}
