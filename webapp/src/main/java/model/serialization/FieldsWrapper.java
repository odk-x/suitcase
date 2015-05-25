package model.serialization;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/25/15
 * Time: 2:49 PM
 */
public class FieldsWrapper implements Serializable {
    @SerializedName("fields")
    private List<Field> mFields;

    public List<Field> getFields() {
        return mFields;
    }

}
