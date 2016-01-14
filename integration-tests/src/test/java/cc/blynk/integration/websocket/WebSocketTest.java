package cc.blynk.integration.websocket;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.websocket.client.WebSocketClient;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.websocket.WebSocketServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketTest extends IntegrationBase {

    private static BaseServer webSocketServer;

    @Before
    public void init() throws Exception {
        if (webSocketServer == null) {
            properties.setProperty("data.folder", getProfileFolder());
            initServerStructures();

            webSocketServer = new WebSocketServer(holder).start();

            sleep(500);
        }
    }

    @Test
    public void testPingOk() throws Exception{
        WebSocketClient webSocketClient = new WebSocketClient("localhost", properties.getIntProperty("tcp.web-socket.port"), false);
        webSocketClient.start(null);
        webSocketClient.send("handshake");
        webSocketClient.send("login 4ae3851817194e2596cf1b7103603ef8");
        webSocketClient.send("ping");
        sleep(600);
    }

}
