package utils;

import model.AggregateTableInfo;

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
  public static boolean isDownloaded(AggregateTableInfo table, String savePath) {
    return Files.exists(getBasePath(table, savePath));
  }

  /**
   * Checks whether a CSV of a table is downloaded
   *
   * @param table
   * @param scanFormatting
   * @param localLink
   * @param extraMeta
   * @param savePath
   * @return
   */
  public static boolean isDownloaded(
      AggregateTableInfo table,
      boolean scanFormatting, boolean localLink, boolean extraMeta,
      String savePath
  ) {
    return Files.exists(getCSVPath(table, scanFormatting, localLink, extraMeta, savePath));
  }

  public static Path getCSVPath(
      AggregateTableInfo table,
      boolean scanFormatting, boolean localLink, boolean extraMeta,
      String savePath
  ) {
    return Paths.get(
        getBasePath(table, savePath).toString(), getCSVName(scanFormatting, localLink, extraMeta));
  }

  public static Path getDefaultSavePath() {
    return Paths.get("Download");
  }

  public static Path getBasePath(AggregateTableInfo table, String savePath) {
    return Paths.get(savePath, table.getAppId(), table.getTableId());
  }

  public static Path getInstancesPath(AggregateTableInfo table, String savePath) {
    return Paths.get(getBasePath(table, savePath).toString(), "instances");
  }

  public static String getCSVName(boolean scanFormatting, boolean localLink, boolean extraMeta) {
    return (localLink ? "data" : "link") +
        (scanFormatting ? "_formatted" : "_unformatted") +
        (extraMeta ? "_extra" : "") + ".csv";
  }

  public static void createBaseDirectory(AggregateTableInfo table, String savePath)
      throws IOException {
    Path basePath = getBasePath(table, savePath);

    if (Files.notExists(basePath)) {
      Files.createDirectories(basePath);
    }
  }

  public static void createInstancesDirectory(AggregateTableInfo table, String savePath)
      throws IOException {
    Path insPath = getInstancesPath(table, savePath);

    if (Files.notExists(insPath)) {
      Files.createDirectories(insPath);
    }
  }

  public static void deleteTableData(AggregateTableInfo table, String savePath) throws IOException {
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
}
