package utils;

import model.SpreedSheetColumn;
import model.SpreedSheetData;
import model.serialization.Column;
import model.serialization.Field;
import model.serialization.Row;
import model.serialization.TableInfo;
import net.RESTClient;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 6:27 PM
 */
public class SpreedSheetBuilder {

    private SpreedSheetData mSheetData;

    public SpreedSheetBuilder(TableInfo tableInfo) throws IOException, JSONException {
        mSheetData = prepareDataForSpreedSheet(tableInfo);
    }

    public String buildSpreedSheet() {
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"table table-striped table-condensed\">");
        html.append("<thead>")
                .append("<tr><th colspan=\"" + SpreedSheetData.COLS_COUNT + "\">"
                        + mSheetData.getFormName() + "</th></tr>");

        int formIndex = 1;
        for (List<SpreedSheetColumn> row : mSheetData.getSheetRows()) {
            // building first three rows
            if (formIndex == 1) {
                html.append("<tr><th></th>");
                for (SpreedSheetColumn column : row) {
                    html.append("<th colspan=\"4\" style=\"text-align:center;\">" + column.getFieldLabelColumn() + "</th>");
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
                html.append("<td>" + column.getFieldLabelColumn() + "</td>");
                html.append("<td>" + column.getRawValueColumn() + "</td>");
                html.append("<td>" + column.getFinalValueColumn() + "</td>");
                String imgValue = column.getImageColumn();
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

    private SpreedSheetData prepareDataForSpreedSheet(TableInfo info) throws IOException, JSONException {
        List<List<SpreedSheetColumn>> sheetRow = new ArrayList<List<SpreedSheetColumn>>();
        for (Row row : info.getRowList()) {
            List<Column> orderedColumns = row.getOrderedColumns();
            List<String> labelColumns = new ArrayList<String>();
            List<String> finalColumns = new ArrayList<String>();
            List<String> imageColumns = new ArrayList<String>();
            String rawJSONFileURL = null;
            for (Column c : orderedColumns) {
                if (JSONUtils.isValidColumn(c.getColumnName())) {
                    if (JSONUtils.isValueColumn(c.getColumnName())) {
                        labelColumns.add(c.getColumnName());
                        finalColumns.add(c.getColumnValue());
                    } else if (JSONUtils.isImgURIColumn(c.getColumnName())) {
                        imageColumns.add(getFullURL(info, row, c));
                    } else if (JSONUtils.isRawJSONColumn(c.getColumnName())) {
                        rawJSONFileURL = getFullURL(info, row, c);
                    }
                }
            }
            List<Field> rawFields = new RESTClient().getRawJSONValue(rawJSONFileURL).getFields();
            sheetRow.add(generateSheetColumn(labelColumns, finalColumns, imageColumns, rawFields));
        }

        return new SpreedSheetData(info.getTableID(), sheetRow);
    }

    private String getFullURL(TableInfo info, Row row, Column c) {
        return info.getInstanceFilesURL() + File.separator + row.getRowID() + File.separator +
                "file/" + c.getColumnValue();
    }

    private List<SpreedSheetColumn> generateSheetColumn(List<String> labels, List<String> finals, List<String> images, List<Field> rawFields) {
        List<SpreedSheetColumn> sheetColumns = new ArrayList<SpreedSheetColumn>();
        Collections.sort(rawFields);
        for (int i = 0; i < SpreedSheetData.COLS_COUNT; i++) {
            if (!rawFields.isEmpty()) {
                sheetColumns.add(new SpreedSheetColumn(labels.get(i), rawFields.get(i).getValue(), finals.get(i), images.get(i)));
            } else {
                sheetColumns.add(new SpreedSheetColumn(labels.get(i), null, finals.get(i), images.get(i)));
            }
        }
        return SortUtils.sort(sheetColumns);
    }
}
