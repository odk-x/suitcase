package model;

import utils.SpreedSheetBuilder;

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
    private List<List<SpreedSheetBuilder.SpreedSheetColumn>> mSpreedSheetColumns;

    public SpreedSheetData(String formName, List<List<SpreedSheetBuilder.SpreedSheetColumn>> spreedSheetColumns) {
        mFormName = formName;
        mSpreedSheetColumns = spreedSheetColumns;
    }

    public String getFormName() {
        return mFormName;
    }

    public List<List<SpreedSheetBuilder.SpreedSheetColumn>> getSpreedSheetColumns() {
        return mSpreedSheetColumns;
    }

}
