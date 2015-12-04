package model.serialization;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/25/15
 * Time: 2:47 PM
 */
public class Field implements Serializable, Comparable<Field> {
    @SerializedName("label")
    private String label;

    @SerializedName("value")
    private String value;

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int compareTo(Field o) {
        return this.getLabel().compareTo(o.getLabel());
    }
}
