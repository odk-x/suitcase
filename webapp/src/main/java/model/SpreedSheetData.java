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

    private String mFormName;
    private List<List<SpreedSheetBuilder.SpreedSheetColumn>> mSpreedSheetColumns;
    private Integer mColumnsNumb;

    public SpreedSheetData(String formName, List<List<SpreedSheetBuilder.SpreedSheetColumn>> spreedSheetColumns) {
        mFormName = formName;
        mSpreedSheetColumns = spreedSheetColumns;
        mColumnsNumb = spreedSheetColumns.get(0).size();
    }

    public String getFormName() {
        return mFormName;
    }

    public void setFormName(String formName) {
        mFormName = formName;
    }

    public List<List<SpreedSheetBuilder.SpreedSheetColumn>> getSpreedSheetColumns() {
        return mSpreedSheetColumns;
    }

    public void setSpreedSheetColumns(List<List<SpreedSheetBuilder.SpreedSheetColumn>> spreedSheetColumns) {
        mSpreedSheetColumns = spreedSheetColumns;
    }

    public Integer getColumnsNumb() {
        return mColumnsNumb;
    }
}
