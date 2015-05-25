package model;

import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/25/15
 * Time: 4:54 PM
 */
public class SpreedSheetRow {
    private List<SpreedSheetColumn> mColumns;

    public SpreedSheetRow(List<SpreedSheetColumn> columns) {
        mColumns = columns;
    }

    public List<SpreedSheetColumn> getColumns() {
        return mColumns;
    }
}
