package cc.blynk.integration.websocket;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.websocket.AppWebSocketClient;
import cc.blynk.server.Holder;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.properties.GCMProperties;
import cc.blynk.utils.properties.MailProperties;
import cc.blynk.utils.properties.SlackProperties;
import cc.blynk.utils.properties.SmsProperties;
import cc.blynk.utils.properties.TwitterProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.utils.StringUtils.WEBSOCKET_WEB_PATH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class AppWebDashboardSocketTest extends BaseTest {

    private static BaseServer hardwareServer;
    private static BaseServer appServer;
    private ClientPair clientPair;
    private static Holder localHolder;

    @AfterClass
    public static void shutdown() {
        hardwareServer.close();
        appServer.close();
        localHolder.close();
    }

    @BeforeClass
    public static void init() throws Exception {
        properties.setProperty("data.folder", getRelativeDataFolder("/profiles"));
        localHolder = new Holder(properties,
                new MailProperties(Collections.emptyMap()),
                new SmsProperties(Collections.emptyMap()),
                new GCMProperties(Collections.emptyMap()),
                new TwitterProperties(Collections.emptyMap()),
                new SlackProperties(Collections.emptyMap()),
                false
        );
        hardwareServer = new HardwareAndHttpAPIServer(localHolder).start();
        appServer = new AppAndHttpsServer(localHolder).start();
    }

    @After
    public void closeLocalHolder() {
        clientPair.stop();
    }

    @Before
    public void intiLocalHolder() throws Exception {
        clientPair = initAppAndHardPair(properties);
    }

    @Test
    public void testAppWebDashSocketlogin() throws Exception{
        AppWebSocketClient appWebSocketClient = new AppWebSocketClient("localhost", properties.getHttpsPort(), WEBSOCKET_WEB_PATH);
        appWebSocketClient.start();
        appWebSocketClient.login(getUserName(), "1");

        appWebSocketClient.verifyResult(ok(1));
        appWebSocketClient.send("ping");
        appWebSocketClient.verifyResult(ok(2));
    }



}
