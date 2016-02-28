package model;

import net.AttachmentManager;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.opendatakit.wink.client.WinkClient.*;

//!!!ATTENTION!!! One per table
public class ODKCsv implements Iterable<String[]> {
  public class ODKCSVIterator implements Iterator<String[]> {
    private int cursor;

    public ODKCSVIterator() {
      this.cursor = 0;
    }

    @Override
    public boolean hasNext() {
      return this.cursor < size;
    }

    @Override
    public String[] next() {
      return next(false, false);
    }

    public String[] next(boolean scanFormatting, boolean localLink) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      String[] nextLine = null;

      try {
        nextLine = get(cursor++, scanFormatting, localLink);
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (!hasNext()) {
        try {
          attMngr.waitForAttachmentDownload();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      return nextLine;
    }

    public int getIndex() {
      return this.cursor;
    }
  }

  private enum Action {
    KEEP,       //No modification
    FILTER,     //Remove
    LINK,       //Convert to a hyperlink
    SCAN_RAW    //Insert Scan's raw output
  }

  //Maps Position to metadata
  private enum Position {
    FRONT, END
  }

  //Some metadata fields are placed before row data, some are placed after
  private static final Map<Position, List<String>> METADATA_POSITION;

  static {
    METADATA_POSITION = new HashMap<>();
    List<String> frontList = new ArrayList<>();
    List<String> endList = new ArrayList<>();

    frontList.add(rowDefId);
    frontList.add(rowDefFormId);
    frontList.add(rowDefLocale);
    frontList.add(rowDefSavepointType);
    frontList.add(rowDefSavepointTimestamp);
    frontList.add(rowDefSavepointCreator);
    METADATA_POSITION.put(Position.FRONT, frontList);

    endList.add(rowDefRowETag);
    endList.add(rowDefFilterType);
    endList.add(rowDefFilterValue);
    METADATA_POSITION.put(Position.END, endList);
  }

  //Maps metadata to its json identifier
  private static final Map<String, String> METADATA_JSON_NAME;

  static {
    METADATA_JSON_NAME = new HashMap<>();

    METADATA_JSON_NAME.put(rowDefId, jsonId);
    METADATA_JSON_NAME.put(rowDefFormId, jsonFormId);
    METADATA_JSON_NAME.put(rowDefLocale, jsonLocale);
    METADATA_JSON_NAME.put(rowDefSavepointType, jsonSavepointType);
    METADATA_JSON_NAME.put(rowDefSavepointTimestamp, jsonSavepointTimestamp);
    METADATA_JSON_NAME.put(rowDefSavepointCreator, jsonSavepointCreator);
    METADATA_JSON_NAME.put(rowDefRowETag, jsonRowETag);
    METADATA_JSON_NAME.put(rowDefFilterType, jsonFilterScope + ": type");
    METADATA_JSON_NAME.put(rowDefFilterValue, jsonFilterScope + ": value");
  }

  private static final String NULL = "null";
  private static final String CONTENT_TYPE_ELEMENT_NAME = "contentType";
  private static final String URI_FRAG_ELEMENT_NAME = "uriFragment";
  private static final String SCAN_RAW_PREFIX = "raw_";
  //Due to optimization and how Scan is designed, 1 column is always filtered
  private static final int NUM_FILTERED = 1; //TODO: generalize this?

  private List<JSONArray> jsonRows;
  private String[] completeCSVHeader;
  //Header of row data only
  private String[] completeDataHeader;
  private int size;
  private Map<String, Action> colAction;
  private AttachmentManager attMngr;
  private AggregateTableInfo table;

  /**
   * Initialize an empty ODKCsv
   *
   * @param attMngr
   * @param table
   */
  public ODKCsv(AttachmentManager attMngr, AggregateTableInfo table) {
    this.size = 0;
    this.attMngr = attMngr;
    this.jsonRows = new ArrayList<>();
    this.table = table;
  }

  /**
   * Initialize ODKCsv with rows
   *
   * @param rows
   * @param attMngr
   * @param table
   * @throws JSONException
   */
  public ODKCsv(JSONArray rows, AttachmentManager attMngr, AggregateTableInfo table)
      throws JSONException {
    if (rows == null) {
      throw new IllegalArgumentException("invalid json");
    }

    this.size = rows.size();

    this.jsonRows = new ArrayList<>();
    this.jsonRows.add(rows);

    this.completeDataHeader = extractHeader(rows.getJSONObject(0));
    this.completeCSVHeader = buildCSVHeader();
    this.colAction = buildActionMap();
    this.attMngr = attMngr;
    this.table = table;
  }

  /**
   * Returns header of csv
   *
   * @param scanFormatting True to retrieve header with scan formatting
   * @return
   */
  public String[] getHeader(boolean scanFormatting) {
    if (this.size < 1) {
      throw new IllegalStateException();
    }

    if (!scanFormatting) {
      return this.completeCSVHeader;
    }

    String[] header = new String[this.completeCSVHeader.length - NUM_FILTERED];
    int offset = 0;
    for (int i = 0; i < this.completeCSVHeader.length; i++) {
      String col = this.completeCSVHeader[i];
      Action act = this.colAction.get(col);
      switch (act) {
      case KEEP:
      case LINK:
        //KEEP and LINK both don't affect header
        header[i - offset] = this.completeCSVHeader[i];
        break;
      case SCAN_RAW:
        header[i - offset] = SCAN_RAW_PREFIX + header[i - offset - 1];
        break;
      case FILTER:
        //remove col from list of header
        offset++;
        break;
      }
    }

    return header;
  }

  /**
   * Tries to merge rows into this ODKCsv instance
   *
   * @param rows
   * @return True if merge is successful
   * @throws JSONException
   */
  public boolean tryAdd(JSONArray rows) throws JSONException {
    if (!isCompatible(rows)) {
      return false;
    }

    if (this.size < 1) {
      //current instance is empty, initialize with rows
      this.completeDataHeader = extractHeader(rows.getJSONObject(0));
      this.completeCSVHeader = buildCSVHeader();
      this.colAction = buildActionMap();
    }

    this.jsonRows.add(rows);
    this.size += rows.size();

    return true;
  }

  /**
   * Retrieves 1 row, including data and metadata
   *
   * @param rowIndex
   * @param scanFormatting
   * @param localLink
   * @return
   * @throws Exception
   */
  public String[] get(int rowIndex, boolean scanFormatting, boolean localLink) throws Exception {
    if (rowIndex >= size) {
      throw new NoSuchElementException();
    }

    int listIndex = 0;
    while (rowIndex >= this.jsonRows.get(listIndex).size()) {
      rowIndex -= this.jsonRows.get(listIndex).size();
      listIndex++;
    }

    JSONObject row = this.jsonRows.get(listIndex).getJSONObject(rowIndex);
    String[] front = getMetadata(row, Position.FRONT);
    String[] middle = getData(row, scanFormatting, localLink);
    String[] end = getMetadata(row, Position.END);

    //TODO: try to avoid copying arrays
    String[] sum = new String[front.length + middle.length + end.length];
    System.arraycopy(front, 0, sum, 0, front.length);
    System.arraycopy(middle, 0, sum, front.length, middle.length);
    System.arraycopy(end, 0, sum, front.length + middle.length, end.length);

    return sum;
  }

  public int getSize() {
    return this.size;
  }

  /**
   * Extract data header from 1 row of JSON
   *
   * @param oneRow
   * @return
   * @throws JSONException
   */
  private String[] extractHeader(JSONObject oneRow) throws JSONException {
    JSONArray orderedColumns = oneRow.getJSONArray(orderedColumnsDef);
    String[] columns = new String[orderedColumns.size()];

    for (int i = 0; i < columns.length; i++) {
      columns[i] = orderedColumns.getJSONObject(i).getString("column");
    }

    return columns;
  }

  /**
   * Builds complete CSV header
   *
   * @return
   */
  private String[] buildCSVHeader() {
    int frontHeaderSize = METADATA_POSITION.get(Position.FRONT).size();
    int endHeaderSize = METADATA_POSITION.get(Position.END).size();
    int headerSize = frontHeaderSize + endHeaderSize + this.completeDataHeader.length;

    String[] header = new String[headerSize];
    for (int i = 0; i < headerSize; i++) {
      String headerCol;

      if (i < frontHeaderSize) {
        headerCol = METADATA_POSITION.get(Position.FRONT).get(i);
      } else if (i < frontHeaderSize + this.completeDataHeader.length) {
        headerCol = this.completeDataHeader[i - frontHeaderSize];
      } else {
        headerCol = METADATA_POSITION.get(Position.END)
            .get(i - frontHeaderSize - this.completeDataHeader.length);
      }

      header[i] = headerCol;
    }

    return header;
  }

  /**
   * Infers if rows is compatible with this ODKCsv instance
   *
   * @param rows
   * @return
   * @throws JSONException
   */
  private boolean isCompatible(JSONArray rows) throws JSONException {
    if (this.size < 1) {
      //this instance is empty -> always compatible
      return true;
    }

    String[] newDataHeader = extractHeader(rows.getJSONObject(0));

    return newDataHeader.length == this.completeDataHeader.length && Arrays
        .deepEquals(newDataHeader, this.completeDataHeader);

  }

  /**
   * Retrieves metadata for 1 row
   *
   * @param row
   * @param pos
   * @return
   * @throws JSONException
   */
  private String[] getMetadata(JSONObject row, Position pos) throws JSONException {
    List<String> metadataList = METADATA_POSITION.get(pos);

    String[] metadata = new String[metadataList.size()];

    for (int i = 0; i < metadata.length; i++) {
      String jsonName = METADATA_JSON_NAME.get(metadataList.get(i));

      if (jsonName.startsWith(jsonFilterScope)) {
        metadata[i] = row.getJSONObject(jsonFilterScope)
            .optString(jsonName.split(":")[1].trim(), NULL);
      } else {
        metadata[i] = row.optString(jsonName, NULL);
      }
    }

    return metadata;
  }

  /**
   * Retrieves data for 1 row
   *
   * @param row
   * @param scanFormatting
   * @param localLink
   * @return
   * @throws Exception
   */
  private String[] getData(JSONObject row, boolean scanFormatting, boolean localLink)
      throws Exception {
    String rowId = row.optString(jsonId);

    ScanJson scanRaw = null;
    if (scanFormatting || localLink) {
      this.attMngr.getListOfRowAttachments(rowId);
      if (scanFormatting) {
        this.attMngr.downloadAttachments(rowId, true);
        scanRaw = new ScanJson(this.attMngr.getScanRawJsonStream(rowId));
      }
    }

    int dataLength = this.completeDataHeader.length;
    if (scanFormatting)
      dataLength -= NUM_FILTERED;
    String[] data = new String[dataLength];

    JSONArray columns = row.getJSONArray(orderedColumnsDef);
    int offset = 0;
    for (int i = 0; i < this.completeDataHeader.length; i++) {
      String colName = this.completeDataHeader[i];
      Action act = this.colAction.get(colName);
      String value = columns.getJSONObject(i).optString("value", NULL);

      switch (act) {
      case KEEP:
        data[i - offset] = value;
        break;
      case FILTER:
        if (!scanFormatting) {
          data[i - offset] = value;
        } else {
          offset++;
          //value is ignored when scanFormatting is high
        }
        break;
      case LINK:
        data[i - offset] = makeLink(value, row, localLink);
        break;
      case SCAN_RAW:
        if (scanFormatting) {
          value = scanRaw.getValue(this.completeDataHeader[i - 1]);
        }
        data[i - offset] = value;
        break;
      }
    }

    if (localLink) {
      this.attMngr.downloadAttachments(rowId, false);
    }

    return data;
  }

  /**
   * Builds "Excel compatible" hyperlink using inferred path
   *
   * @param fileName
   * @param row
   * @param localLink
   * @return
   * @throws IOException
   */
  private String makeLink(String fileName, JSONObject row, boolean localLink) throws IOException {
    String template = "=HYPERLINK(\"%s\", \"Ctrl + Click to view\")";
    String attachmentUrlStr;
    if (localLink) {
      URL attachmentUrl = this.attMngr.getAttachmentUrl(row.optString(jsonId), fileName, true);
      if (attachmentUrl == null) {
        return "File is missing on Aggregate server.";
      }
      attachmentUrlStr = attachmentUrl.toString();
    } else {
      attachmentUrlStr =
          this.table.getServerUrl() + "/" + "tables" + "/" + this.table.getAppId() + "/" +
              this.table.getTableId() + "/ref/" + this.table.getSchemaETag() + "/attachments/" +
              row.optString(jsonId) + "/file/" + fileName;
    }

    return String.format(template, attachmentUrlStr);
  }

  /**
   * Associates column name to its Action
   *
   * @return
   */
  private Map<String, Action> buildActionMap() {
    Map<String, Action> actionMap = new HashMap<>();

    for (String s : this.completeCSVHeader) {
      if (s.endsWith(CONTENT_TYPE_ELEMENT_NAME)) {
        if (s.equals("raw_contentType")) {
          actionMap.put(s, Action.FILTER); //TODO: handle this better
        } else {
          actionMap.put(s, Action.SCAN_RAW);
        }
      } else if (s.endsWith(URI_FRAG_ELEMENT_NAME)) {
        actionMap.put(s, Action.LINK);
      } else {
        actionMap.put(s, Action.KEEP);
      }
    }

    return actionMap;
  }

  @Override
  public Iterator<String[]> iterator() {
    return getODKCSVIterator();
  }

  public ODKCSVIterator getODKCSVIterator() {
    return new ODKCSVIterator();
  }
}
