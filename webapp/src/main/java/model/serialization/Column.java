package model.serialization;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 5:11 PM
 */
public class Column implements Serializable {
    @SerializedName("column")
    private String mColumnName;
    @SerializedName("value")
    private String mColumnValue;

    public String getColumnName() {
        return mColumnName;
    }

    public void setColumnName(String columnName) {
        mColumnName = columnName;
    }

    public String getColumnValue() {
        return mColumnValue;
    }

    public void setColumnValue(String columnValue) {
        mColumnValue = columnValue;
    }
}
