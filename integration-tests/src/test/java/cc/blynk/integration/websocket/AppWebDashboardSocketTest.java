package cc.blynk.integration.websocket;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.websocket.AppWebSocketClient;
import cc.blynk.server.Holder;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.properties.GCMProperties;
import cc.blynk.utils.properties.MailProperties;
import cc.blynk.utils.properties.SmsProperties;
import cc.blynk.utils.properties.TwitterProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static cc.blynk.utils.StringUtils.WEBSOCKET_WEB_PATH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class AppWebDashboardSocketTest extends IntegrationBase {

    private static BaseServer hardwareServer;
    private static BaseServer appServer;
    private static ClientPair clientPair;
    private static Holder localHolder;

    @AfterClass
    public static void shutdown() throws Exception {
        hardwareServer.close();
        appServer.close();
        clientPair.stop();
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
                false
        );
        hardwareServer = new HardwareAndHttpAPIServer(localHolder).start();
        appServer = new AppAndHttpsServer(localHolder).start();
        clientPair = initAppAndHardPair(httpsPort, httpPort, properties);
    }

    @Test
    public void testAppWebDashSocketlogin() throws Exception{
        AppWebSocketClient appWebSocketClient = new AppWebSocketClient("localhost", httpsPort, WEBSOCKET_WEB_PATH);
        appWebSocketClient.start();
        appWebSocketClient.login(DEFAULT_TEST_USER, "1");

        appWebSocketClient.verifyResult(ok(1));
        appWebSocketClient.send("ping");
        appWebSocketClient.verifyResult(ok(2));
    }



}
