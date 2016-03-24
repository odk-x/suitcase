package utils;

import model.AggregateTableInfo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtils {
  public static boolean isDownloaded(AggregateTableInfo table) {
    return Files.exists(getBasePath(table));
  }

  public static boolean isDownloaded(AggregateTableInfo table, boolean scanFormatting,
      boolean localLink, boolean extraMeta) {
    return Files.exists(getCSVPath(table, scanFormatting, localLink, extraMeta));
  }

  public static Path getCSVPath(AggregateTableInfo table, boolean scanFormatting,
      boolean localLink, boolean extraMeta) {
    return Paths.get(getBasePath(table).toString(), getCSVName(scanFormatting, localLink, extraMeta));
  }

  public static Path getBasePath(AggregateTableInfo table) {
    return Paths.get("Download", table.getAppId(), table.getTableId());
  }

  public static Path getInstancesPath(AggregateTableInfo table) {
    return Paths.get(getBasePath(table).toString(), "instances");
  }

  public static String getCSVName(boolean scanFormatting, boolean localLink, boolean extraMeta) {
    return (localLink ? "data" : "link") + (scanFormatting ? "_formatted" : "_unformatted")
        + (extraMeta ? "_extra" : "") + ".csv";
  }

  public static void createBaseDirectory(AggregateTableInfo table) throws IOException {
    if (Files.notExists(getBasePath(table))) {
      Files.createDirectories(getBasePath(table));
    }
  }

  public static void createInstancesDirectory(AggregateTableInfo table) throws IOException {
    if (Files.notExists(getInstancesPath(table))) {
      Files.createDirectories(getInstancesPath(table));
    }
  }

  public static void deleteTableData(AggregateTableInfo table) throws IOException {
    Files.walkFileTree(getBasePath(table), new SimpleFileVisitor<Path>() {
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
