package model.serialization;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 5:33 PM
 */
public class RowsData implements Serializable {
    @SerializedName("rows")
    private List<Row> mRows;

    public List<Row> getRows() {
        return mRows;
    }

    public void setRows(List<Row> rows) {
        mRows = rows;
    }
}
