package model;

import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 6:30 PM
 */
public class SpreedSheetData {
    public static final String[] LABELS_ROW = {"Form Number", "Field Label", "Raw Value", "Final Value", "Image"};

    // pre defined amount
    // addtl_text column is included
    public static final Integer COLS_COUNT = 62;
    private String mFormName;
    private List<SpreedSheetRow> mSheetRows;

    public SpreedSheetData(String formName, List<SpreedSheetRow> spreedSheetColumns) {
        mFormName = formName;
        mSheetRows = spreedSheetColumns;
    }

    public String getFormName() {
        return mFormName;
    }

    public List<SpreedSheetRow> getSheetRows() {
        return mSheetRows;
    }

}
