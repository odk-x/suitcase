import model.serialization.RowsData;
import model.serialization.TableInfo;
import net.RESTClient;
import org.json.JSONException;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerRoute;
import utils.SpreedSheetBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;

public class ReportServer {

    public static void main(String[] args) {
        get(new FreeMarkerRoute("/") {
            @Override
            public ModelAndView handle(Request request, Response response) {
                Map<String, Object> viewObjects = new HashMap<String, Object>();
                viewObjects.put("templateName", "spreedsheet.ftl");
                RESTClient client = new RESTClient();

                try {
                    TableInfo tableInfo = client.getTableResource();
                    RowsData rows = client.getAllDataRows(tableInfo.getSchemaTag());
                    tableInfo.setRowList(rows.getRows());

                    SpreedSheetBuilder builder = new SpreedSheetBuilder(tableInfo);
                    viewObjects.put("spreedsheet", builder.buildSpreedSheet());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                return modelAndView(viewObjects, "layout.ftl");
            }
        });
    }
}
