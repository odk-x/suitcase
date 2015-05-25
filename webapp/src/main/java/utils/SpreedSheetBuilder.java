package utils;

import model.SpreedSheetData;
import model.serialization.Column;
import model.serialization.Row;
import model.serialization.TableInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 6:27 PM
 */
public class SpreedSheetBuilder {

    private SpreedSheetData mSheetData;

    public SpreedSheetBuilder(TableInfo tableInfo) {
        mSheetData = prepareDataForSpreedSheet(tableInfo);
    }

    public String buildSpreedSheet() {
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"table table-striped table-condensed\">");
        html.append("<thead>")
                .append("<tr><th colspan=\"" + SpreedSheetData.COLS_COUNT + "\">"
                        + mSheetData.getFormName() + "</th></tr>");

        int formIndex = 1;
        for (List<SpreedSheetColumn> row : mSheetData.getSpreedSheetColumns()) {
            // building first three rows
            if (formIndex == 1) {
                html.append("<tr><th></th>");
                for (SpreedSheetColumn column : row) {
                    html.append("<th colspan=\"4\" style=\"text-align:center;\">" + column.getFieldLabelColumn().getValue() + "</th>");
                }
                html.append("</tr>");
                html.append("<tr class=\"info\"><th>" + SpreedSheetData.LABELS_ROW[0] + "</th>");
                for (int i = 0; i < SpreedSheetData.COLS_COUNT; i++) {
                    html.append("<th>" + SpreedSheetData.LABELS_ROW[1] + "</th>");
                    html.append("<th>" + SpreedSheetData.LABELS_ROW[2] + "</th>");
                    html.append("<th>" + SpreedSheetData.LABELS_ROW[3] + "</th>");
                    html.append("<th>" + SpreedSheetData.LABELS_ROW[4] + "</th>");
                }
                html.append("</tr>").append("</thead>").append("<tbody>");
            }
            // building rows with data
            html.append("<tr><td style=\"text-align:center;font-family:Arial, sans-serif;font-size:35px;font-weight:bold;\">" + formIndex + "</td>");
            for (SpreedSheetColumn column : row) {
                html.append("<td>" + column.getFieldLabelColumn().getValue() + "</td>");
                html.append("<td>" + "TBD"/*column.getRawValueColumn().getValue()*/ + "</td>");
                html.append("<td>" + column.getFinalValueColumn().getValue() + "</td>");
                String imgValue = column.getImageColumn().getValue();
                if (imgValue == null) {
                    html.append("<td></td>");
                } else {
                    html.append("<td><img src=\"" + imgValue + "\"></td>");
                }
            }
            html.append("</tr>");
            formIndex++;
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private SpreedSheetData prepareDataForSpreedSheet(TableInfo info) {
        List<List<SpreedSheetColumn>> sheetRow = new ArrayList<List<SpreedSheetColumn>>();
        for (Row row : info.getRowList()) {
            List<Column> orderedColumns = row.getOrderedColumns();
            List<AbstractColumn> labelColumns = new ArrayList<AbstractColumn>();
            List<AbstractColumn> finalColumns = new ArrayList<AbstractColumn>();
            List<AbstractColumn> imageColumns = new ArrayList<AbstractColumn>();
            String rawJSONFileURL = null;
            for (Column c : orderedColumns) {
                if (JSONUtils.isValidColumn(c.getColumnName())) {
                    if (JSONUtils.isValueColumn(c.getColumnName())) {
                        labelColumns.add(new AbstractColumn(c.getColumnName()));
                        finalColumns.add(new AbstractColumn(c.getColumnValue()));
                    } else if (JSONUtils.isImgURIColumn(c.getColumnName())) {
                        imageColumns.add(new AbstractColumn(getFullURL(info, row, c)));
                    } else if (JSONUtils.isRawJSONColumn(c.getColumnName())) {
                        rawJSONFileURL = getFullURL(info, row, c);
                    }
                }
            }
            sheetRow.add(generateSheetColumn(labelColumns, finalColumns, imageColumns));
        }

        return new SpreedSheetData(info.getTableID(), sheetRow);
    }

    private String getFullURL(TableInfo info, Row row, Column c) {
        return info.getInstanceFilesURL() + File.separator + row.getRowID() + File.separator +
                "file/" + c.getColumnValue();
    }


    private List<SpreedSheetColumn> generateSheetColumn(List<AbstractColumn> labels, List<AbstractColumn> finals, List<AbstractColumn> images) {
        List<SpreedSheetColumn> sheetColumns = new ArrayList<SpreedSheetColumn>();
        for (int i = 0; i < SpreedSheetData.COLS_COUNT; i++) {
            sheetColumns.add(new SpreedSheetColumn(labels.get(i), null, finals.get(i), images.get(i)));
        }
        return sheetColumns;
    }


    public class SpreedSheetColumn {
        private AbstractColumn mFieldLabelColumn;
        private AbstractColumn mRawValueColumn;
        private AbstractColumn mFinalValueColumn;
        private AbstractColumn mImageColumn;

        public SpreedSheetColumn(AbstractColumn fieldLabelColumn, AbstractColumn rawValueColumn, AbstractColumn finalValueColumn, AbstractColumn imageColumn) {
            mFieldLabelColumn = fieldLabelColumn;
            mRawValueColumn = rawValueColumn;
            mFinalValueColumn = finalValueColumn;
            mImageColumn = imageColumn;
        }

        public AbstractColumn getFieldLabelColumn() {
            return mFieldLabelColumn;
        }

        public AbstractColumn getRawValueColumn() {
            return mRawValueColumn;
        }

        public AbstractColumn getFinalValueColumn() {
            return mFinalValueColumn;
        }

        public AbstractColumn getImageColumn() {
            return mImageColumn;
        }

        @Override
        public String toString() {
            return mFieldLabelColumn.getValue().toString();
        }
    }

    public class AbstractColumn {
        protected String value;

        public AbstractColumn(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
