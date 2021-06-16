package org.opendatakit.suitcase.test;

import org.assertj.swing.core.ComponentLookupScope;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.testing.AssertJSwingTestCaseTemplate;
import org.assertj.swing.timing.Pause;
import org.junit.*;
import org.opendatakit.suitcase.Suitcase;
import org.opendatakit.suitcase.model.CloudEndpointInfo;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;

import static org.assertj.swing.finder.WindowFinder.findFrame;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

public class LoginGUITest extends AssertJSwingTestCaseTemplate {

    private FrameFixture frame;
    private CloudEndpointInfo cloudEndpointInfo = null;
    private String serverUrl = null;
    private String appId = null;
    private String absolutePathOfTestFiles = null;
    private String userName = null;
    private String password = null;
    private String version = null;
    @BeforeClass
    public static final void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public final void setUp() throws MalformedURLException {
        // call provided AssertJSwingTestCaseTemplate.setUpRobot()
        this.setUpRobot();
        robot().settings().componentLookupScope(ComponentLookupScope.ALL);
        // initialize the graphical user interface
        application(Suitcase.class).start();

        serverUrl = System.getProperty("test.aggUrl");
        appId = System.getProperty("test.appId");
        absolutePathOfTestFiles = System.getProperty("test.absolutePathOfTestFiles");
        userName = System.getProperty("test.userName");
        password = System.getProperty("test.password");
        version = "2";

        cloudEndpointInfo = new CloudEndpointInfo(serverUrl, appId, userName, password);
        this.frame = findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
            protected boolean isMatching(Frame frame) {
                return "org.opendatakit.suitcase.Suitcase".equals(frame.getTitle()) && frame.isShowing();
            }
        }).using(robot());
        this.frame.show();
        this.frame.resizeTo(new Dimension(600, 600));
    }

    @Test
    public void test1() {
    JPanelFixture ioPanelFixture = frame.panel(new GenericTypeMatcher<JPanel>(JPanel.class) {
            @Override
            protected boolean isMatching(JPanel panel) {
                return "io_panel".equals(panel.getName());
            }
        });
     frame.textBox("username").setText(userName);
     frame.textBox("app_id").setText(appId);
     frame.textBox("password").setText(password);
     frame.textBox("server_url").setText(serverUrl);
     ioPanelFixture.requireNotVisible();          // IOPanel should not be visible on starting the application
     frame.panel("main_panel").requireVisible();  // MainPanel should be visible on start of the application
     frame.button("login_button").click();
     Pause.pause(15000);                          // Wait for 15 seconds for login to complete
     ioPanelFixture.requireVisible();             // IOPanel should be visible after login
    }

    @After
    public final void tearDown() {
        try {
            this.frame = null;
        } finally {
            cleanUp();
        }
    }

}