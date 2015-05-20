package model.serialization;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 5:11 PM
 */
public class Row implements Serializable {
    @SerializedName("id")
    private String mRowID;
    @SerializedName("orderedColumns")
    private List<Column> mOrderedColumns;

    public String getRowID() {
        return mRowID;
    }

    public void setRowID(String rowID) {
        mRowID = rowID;
    }

    public List<Column> getOrderedColumns() {
        return mOrderedColumns;
    }

    public void setOrderedColumns(List<Column> orderedColumns) {
        mOrderedColumns = orderedColumns;
    }
}
