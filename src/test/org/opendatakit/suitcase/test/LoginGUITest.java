package org.opendatakit.suitcase.test;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTestRunner;
import org.assertj.swing.core.ComponentLookupScope;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.*;
import org.assertj.swing.testing.AssertJSwingTestCaseTemplate;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.opendatakit.suitcase.Suitcase;
import org.opendatakit.suitcase.net.SyncWrapper;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.swing.finder.WindowFinder.findFrame;
import static org.assertj.swing.launcher.ApplicationLauncher.application;
import static org.awaitility.Awaitility.await;

@RunWith(CacioTestRunner.class)
public class LoginGUITest extends AssertJSwingTestCaseTemplate {

    private static final String PASSWORD_EMPTY_ERROR = "Password cannot be empty.";
    private static final String SERVER_URL_EMPTY_ERROR = "Cloud Endpoint address cannot be empty.";
    private static final String USERNAME_URL_EMPTY_ERROR = "Username cannot be empty.";
    private static final String APP_ID_EMPTY_ERROR = "App Id cannot be empty.";
    private FrameFixture frame;
    private String serverUrl = null;
    private String appId = null;
    private String userName = null;
    private String password = null;
    @BeforeClass
    public static final void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public final void setUp() {
        // call provided AssertJSwingTestCaseTemplate.setUpRobot()
        this.setUpRobot();
        robot().settings().componentLookupScope(ComponentLookupScope.ALL);
        // initialize the graphical user interface
        application(Suitcase.class).start();

        serverUrl = System.getProperty("test.aggUrl");
        appId = System.getProperty("test.appId");
        userName = System.getProperty("test.userName");
        password = System.getProperty("test.password");

        this.frame = findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
            protected boolean isMatching(Frame frame) {
                return "org.opendatakit.suitcase.Suitcase".equals(frame.getTitle()) && frame.isShowing();
            }
        }).using(robot());
        this.frame.show();
        Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(Duration.TEN_SECONDS);
    }

    @Test
    public void loginErrorTest() {
        JPanelFixture ioPanelFixture = frame.panel(new GenericTypeMatcher<JPanel>(JPanel.class) {
            @Override
            protected boolean isMatching(JPanel panel) {
                return "io_panel".equals(panel.getName());
            }
        });
        frame.textBox("username").setText("test");
        frame.textBox("app_id").setText("test");
        frame.textBox("password").setText("");
        frame.textBox("server_url").setText("test");
        frame.button("login_button").click();
        JOptionPaneFixture jOptionPaneFixture = JOptionPaneFinder.findOptionPane(new GenericTypeMatcher<JOptionPane>(JOptionPane.class) {
            @Override
            protected boolean isMatching(JOptionPane pane) {
                if (pane.isShowing()) {      // isShowing() should be true because multiple instances of path chooser is initialized across different tabs
                    return true;
                } else {
                    return false;
                }
            }
        }).using(robot());
        jOptionPaneFixture.requireMessage(PASSWORD_EMPTY_ERROR);
    }

    @Test
    public void loginTest() {
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
     await().until(SyncWrapper.getInstance()::isInitialized);                          // Wait for login to complete
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