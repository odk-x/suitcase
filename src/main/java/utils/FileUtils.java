package utils;

import model.AggregateTableInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    public static boolean isDownloaded(AggregateTableInfo table, boolean scanFormatting, boolean localLink) {
        return Files.exists(getCSVPath(table, scanFormatting, localLink));
    }

    public static Path getCSVPath(AggregateTableInfo table, boolean scanFormatting, boolean localLink) {
        return Paths.get(
                getBasePath(table).toString(),
                getCSVName(scanFormatting, localLink)
        );
    }

    public static Path getBasePath(AggregateTableInfo table) {
        return Paths.get(
                "Download",
                table.getAppId(),
                table.getTableId()
        );
    }

    public static Path getInstancesPath(AggregateTableInfo table) {
        return Paths.get(
                getBasePath(table).toString(),
                "instances"
        );
    }

    public static String getCSVName(boolean scanFormatting, boolean localLink) {
        return (localLink ? "data" : "link") + (scanFormatting ? "_formatted" : "_unformatted") + ".csv";
    }

    public static void createDiretoryStructure(AggregateTableInfo table) throws IOException {
        if (Files.notExists(getBasePath(table))) {
            Files.createDirectories(getBasePath(table));
        }
    }

    public static void createInstancesDirectory(AggregateTableInfo table) throws IOException {
        if (Files.notExists(getInstancesPath(table))) {
            Files.createDirectories(getInstancesPath(table));
        }
    }
}
