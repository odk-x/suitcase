package model.serialization;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 5:38 PM
 */
public class TableInfo implements Serializable {
    @SerializedName("schemaETag")
    private String mSchemaTag;
    @SerializedName("tableId")
    private String mTableID;
    @SerializedName("instanceFilesUri")
    private String mInstanceFilesURL;

    private List<Row> mRowList;

    public TableInfo() {
    }

    public TableInfo(String schemaTag, String tableID, String instanceFilesURL) {
        mSchemaTag = schemaTag;
        mTableID = tableID;
        mInstanceFilesURL = instanceFilesURL;
        mRowList = new ArrayList<Row>();
    }

    public String getSchemaTag() {
        return mSchemaTag;
    }

    public void setSchemaTag(String schemaTag) {
        mSchemaTag = schemaTag;
    }

    public String getTableID() {
        return mTableID;
    }

    public void setTableID(String tableID) {
        mTableID = tableID;
    }

    public String getInstanceFilesURL() {
        return mInstanceFilesURL;
    }

    public void setInstanceFilesURL(String instanceFilesURL) {
        mInstanceFilesURL = instanceFilesURL;
    }

    public List<Row> getRowList() {
        return mRowList;
    }

    public void setRowList(List<Row> rowList) {
        mRowList = rowList;
    }
}
