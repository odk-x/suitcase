import static spark.Spark.*;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.freemarker.FreeMarkerRoute;

import java.util.*;

public class ReportServer {

    public static void main(String[] args) {
        get(new FreeMarkerRoute("/") {
            @Override
            public ModelAndView handle(Request request, Response response) {
                Map<String, Object> viewObjects = new HashMap<String, Object>();

              
                viewObjects.put("templateName", "index.ftl");
                return modelAndView(viewObjects, "layout.ftl");
            }
        });
    }
}
