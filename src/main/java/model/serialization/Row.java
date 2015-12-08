//package model.serialization;
//
//import com.google.gson.annotations.SerializedName;
//
//import java.io.Serializable;
//import java.util.List;
//
///**
// * Created by Kamil Kalfas
// * kkalfas@soldevelo.com
// * Date: 5/20/15
// * Time: 5:11 PM
// */
//public class Row implements Serializable {
//    @SerializedName("id")
//    private String mRowID;
//    @SerializedName("orderedColumns")
//    private List<Column> mOrderedColumns;
//
//    private String mJSONFile;
//
//    public String getJSONFile() {
//        return mJSONFile;
//    }
//
//    public void setJSONFile(String JSONFile) {
//        mJSONFile = JSONFile;
//    }
//
//    public String getRowID() {
//        return mRowID;
//    }
//
//    public List<Column> getOrderedColumns() {
//        return mOrderedColumns;
//    }
//
//}
