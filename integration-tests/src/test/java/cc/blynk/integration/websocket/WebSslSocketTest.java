package cc.blynk.integration.websocket;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.websocket.WebSocketClient;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.utils.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Response.OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSslSocketTest extends BaseTest {

    private static BaseServer webSocketServer;
    public static int sslWebSocketPort = httpsPort;

    @AfterClass
    public static void shutdown() throws Exception {
        webSocketServer.close();
    }

    @Before
    public void init() throws Exception {
        if (webSocketServer == null) {
            webSocketServer = new AppAndHttpsServer(holder).start();
        }
    }

    @Override
    public String getDataFolder() {
        return getRelativeDataFolder("/profiles");
    }

    @Test
    public void testBasicWebSocketCommandsOk() throws Exception{
        WebSocketClient webSocketClient = new WebSocketClient("localhost", sslWebSocketPort, StringUtils.WEBSOCKET_PATH, true);
        webSocketClient.start();
        webSocketClient.send("login 4ae3851817194e2596cf1b7103603ef8");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        webSocketClient.send("ping");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
    }

}
