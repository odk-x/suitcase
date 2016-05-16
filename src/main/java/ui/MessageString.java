package ui;

public class MessageString {
  public static final String NEW_LINE = System.lineSeparator();

  // ODK names
  private static final String AGGREGATE = "Aggregate";
  private static final String SUITCASE = "Suitcase";
  private static final String TABLE_ID = "Table ID";
  private static final String APP_ID = "App ID";

  // Error message
  public static final String GENERIC_ERR = "Error occurred";
  public static final String BAD_URL = AGGREGATE + " address is invalid.";
  public static final String BAD_CRED = AGGREGATE + " address, " + APP_ID + ", username, or password is invalid. Please check your credentials.";
  public static final String DATA_DIR_INVALID = "Data directory does not contain \"assets\" directory or \"tables\" directory.";
  public static final String JSON_PARSE_ERROR = "Please visit" + AGGREGATE + "web interface for error detail.";

  // unable to ... error
  private static final String UNABLE_PREFIX = "Unable to ";
  public static final String IO_DELETE_ERR = UNABLE_PREFIX + "delete data.";
  public static final String IO_WRITE_ERR = UNABLE_PREFIX + "write file.";

  // ... does not exist error
  private static final String NONEXISTENCE_POSTFIX = " does not exist";
  public static final String BAD_TABLE_ID = TABLE_ID + NONEXISTENCE_POSTFIX;
  public static final String DATA_DIR_NOT_EXIST = "Data directory" + NONEXISTENCE_POSTFIX;

  // ... cannot be empty error
  private static final String CANNOT_EMPTY_POSTFIX = " cannot be empty.";
  public static final String AGG_EMPTY = AGGREGATE + " address" + CANNOT_EMPTY_POSTFIX;
  public static final String APP_ID_EMPTY = APP_ID + CANNOT_EMPTY_POSTFIX;
  public static final String TABLE_ID_EMPTY = TABLE_ID + CANNOT_EMPTY_POSTFIX;
  public static final String USERNAME_EMPTY = "Username" + CANNOT_EMPTY_POSTFIX;
  public static final String PASSWORD_EMPTY = "Password" + CANNOT_EMPTY_POSTFIX;
  public static final String VERSION_EMPTY = "Version" + CANNOT_EMPTY_POSTFIX;
  public static final String DATA_PATH_EMPTY = "Data path" + CANNOT_EMPTY_POSTFIX;
  public static final String SAVE_PATH_EMPTY = "Save path" + CANNOT_EMPTY_POSTFIX;

  // Prompts
  public static final String OVERWRITE_DATA = "Data from a previous session detected. Delete existing data and download data from Aggregate server?";
  public static final String OVERWRITE_CSV = "This CSV has been downloaded. Delete existing CSV and download data from Aggregate server?";
}
