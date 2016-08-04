package org.opendatakit.suitcase.ui;

public class ProgressBarStatus {
  private Integer progress;
  private String string;
  private Boolean indeterminate;

  public ProgressBarStatus(Integer progress, String string, Boolean indeterminate) {
    this.progress = progress;
    this.string = string;
    this.indeterminate = indeterminate;
  }

  public Integer getProgress() {
    return progress;
  }

  public String getString() {
    return string;
  }

  public Boolean isIndeterminate() {
    return indeterminate;
  }
}
