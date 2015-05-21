package model.serialization;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
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

    public String getSchemaTag() {
        return mSchemaTag;
    }

    public String getTableID() {
        return mTableID;
    }

    public String getInstanceFilesURL() {
        return mInstanceFilesURL;
    }

    public List<Row> getRowList() {
        return mRowList;
    }

    public void setRowList(List<Row> rowList) {
        mRowList = rowList;
    }
}
