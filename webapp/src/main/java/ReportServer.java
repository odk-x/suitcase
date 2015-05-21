import model.serialization.RowsData;
import model.serialization.TableInfo;
import net.RESTClient;
import org.json.JSONException;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.freemarker.FreeMarkerRoute;
import utils.SpreedSheetBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;

public class ReportServer {
    static RESTClient client = new RESTClient();
    static TableInfo tableInfo;
    static RowsData rows;

    public static void main(String[] args) {
        downloadData();

        final SpreedSheetBuilder builder = new SpreedSheetBuilder(tableInfo);

        buildJFrame();

        get(new FreeMarkerRoute("/") {
            @Override
            public ModelAndView handle(Request request, Response response) {
                Map<String, Object> viewObjects = new HashMap<String, Object>();
                viewObjects.put("templateName", "spreedsheet.ftl");
                viewObjects.put("spreedsheet", builder.buildSpreedSheet());

                return modelAndView(viewObjects, "layout.ftl");
            }
        });
    }

    private static void downloadData() {
        try {
            tableInfo = client.getTableResource();
            rows = client.getAllDataRows(tableInfo.getSchemaTag());
            tableInfo.setRowList(rows.getRows());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private static void buildJFrame() {
        final JFrame frame = new JFrame("M&E Report");
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JLabel label = new JLabel("Stop server");
        JButton button = new JButton();

        button.setText("Stop");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });
        panel.add(label);
        panel.add(button);
        frame.add(panel);
        frame.setSize(300, 300);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
