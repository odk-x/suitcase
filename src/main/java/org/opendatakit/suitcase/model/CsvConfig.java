package org.opendatakit.suitcase.model;

public class CsvConfig {
  private boolean downloadAttachment;
  private boolean scanFormatting;
  private boolean extraMetadata;

  public CsvConfig(boolean downloadAttachment, boolean scanFormatting, boolean extraMetadata) {
    this.downloadAttachment = downloadAttachment;
    this.scanFormatting = scanFormatting;
    this.extraMetadata = extraMetadata;
  }

  public CsvConfig() {
    this(false, false, false);
  }

  public boolean isDownloadAttachment() {
    return downloadAttachment;
  }

  public boolean isScanFormatting() {
    return scanFormatting;
  }

  public boolean isExtraMetadata() {
    return extraMetadata;
  }

  @Override
  public String toString() {
    return "CsvConfig{" +
        "downloadAttachment=" + downloadAttachment +
        ", scanFormatting=" + scanFormatting +
        ", extraMetadata=" + extraMetadata +
        '}';
  }
}
