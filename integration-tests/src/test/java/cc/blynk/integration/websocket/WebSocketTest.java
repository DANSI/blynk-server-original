package cc.blynk.integration.websocket;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.websocket.AppWebSocketClient;
import cc.blynk.integration.model.websocket.WebSocketClient;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static cc.blynk.utils.StringUtils.WEBSOCKET_WEB_PATH;
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
public class WebSocketTest extends BaseTest {

    private BaseServer webSocketServer;
    private BaseServer appServer;
    private ClientPair clientPair;
    //private static Holder localHolder;

    //web socket ports
    private static int tcpWebSocketPort;

    @After
    public void shutdown() {
        webSocketServer.close();
        appServer.close();
        clientPair.stop();
        holder.close();
    }

    @Before
    public void init() throws Exception {
        tcpWebSocketPort = properties.getHttpPort();
        webSocketServer = new HardwareAndHttpAPIServer(holder).start();
        appServer = new MobileAndHttpsServer(holder).start();
        clientPair = initAppAndHardPair(properties);
    }

    @Override
    public String getDataFolder() {
        return getRelativeDataFolder("/profiles");
    }

    @Test
    public void testAppWebDashSocketLogin() throws Exception{
        AppWebSocketClient appWebSocketClient = new AppWebSocketClient("localhost", properties.getHttpsPort(), WEBSOCKET_WEB_PATH);
        appWebSocketClient.start();
        appWebSocketClient.login(getUserName(), "1");

        appWebSocketClient.verifyResult(ok(1));
        appWebSocketClient.send("ping");
        appWebSocketClient.verifyResult(ok(2));
    }

    @Test
    public void testBasicWebSocketCommandsOk2() throws Exception{
        WebSocketClient webSocketClient = new WebSocketClient("localhost", tcpWebSocketPort, "/websockets", false);
        webSocketClient.start();
        webSocketClient.send("login 4ae3851817194e2596cf1b7103603ef8");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        webSocketClient.send("ping");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testBasicWebSocketCommandsOk() throws Exception{
        WebSocketClient webSocketClient = new WebSocketClient("localhost", tcpWebSocketPort, StringUtils.WEBSOCKETS_PATH, false);
        webSocketClient.start();
        webSocketClient.send("login 4ae3851817194e2596cf1b7103603ef8");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        webSocketClient.send("ping");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testSslBasicWebSocketCommandsOk() throws Exception{
        WebSocketClient webSocketClient = new WebSocketClient("localhost", properties.getHttpsPort(), StringUtils.WEBSOCKETS_PATH, true);
        webSocketClient.start();
        webSocketClient.send("login 4ae3851817194e2596cf1b7103603ef8");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        webSocketClient.send("ping");
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testSyncBetweenWebSocketsAndAppWorks() throws Exception {
        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        WebSocketClient webSocketClient = new WebSocketClient("localhost", tcpWebSocketPort, StringUtils.WEBSOCKETS_PATH, false);
        webSocketClient.start();
        webSocketClient.send("login " + clientPair.token);
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1-0 vw 4 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 1"))));
        verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 1"))));

        webSocketClient.send("hardware vw 4 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 4 2"))));

        clientPair.hardwareClient.send("hardware vw 4 3");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 4 3"))));

        clientPair.appClient.reset();
        WebSocketClient webSocketClient2 = new WebSocketClient("localhost", tcpWebSocketPort, StringUtils.WEBSOCKETS_PATH, false);
        webSocketClient2.start();
        webSocketClient2.send("login " + clientPair.token);
        verify(webSocketClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(webSocketClient2.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("pm 1 out 2 out 3 out 5 out 6 in 7 in 30 in 8 in"))));
        webSocketClient2.msgId = 1000;

        for (int i = 1; i <= 10; i++) {
            clientPair.appClient.send("hardware 1-0 vw 4 " + i);
            verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(i, HARDWARE, b("vw 4 " + i))));
            verify(webSocketClient.responseMock, timeout(500)).channelRead(any(), eq(produce(i, HARDWARE, b("vw 4 " + i))));
            verify(webSocketClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(i, HARDWARE, b("vw 4 " + i))));
            webSocketClient2.send("hardsync " + b("vr 4"));
            verify(webSocketClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1000 + i, HARDWARE, b("vw 4 " + i))));
        }
    }

    @Test
    public void testSyncBetweenWebSocketsAndAppWorksLoop() throws Exception {
        for (int i = 0; i < 10; i++) {
            testSyncBetweenWebSocketsAndAppWorks();
        }
    }

}
