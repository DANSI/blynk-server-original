package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPushLogicTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
        this.appServer = new AppServer(holder).start(transportTypeHolder);

        this.clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void addPushTokenWrongInput()  throws Exception  {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.send("login test@test.com 1 Android" + "\0" + "RC13");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        appClient.send("createDash {\"id\":1, \"createdAt\":1, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        appClient.send("addPushToken 1\0uid\0token");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, NOT_ALLOWED)));
    }

    @Test
    public void addPushTokenWrongInput2() throws Exception {
        clientPair.appClient.send("addPushToken 1\0uid\0token");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        doThrow(new Exception("NotRegistered")).when(gcmWrapper).send(any());

        clientPair.hardwareClient.send("push Yo!");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOTIFICATION_EXCEPTION)));


        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);

        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        assertNotNull(notification);
        assertEquals(0, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        clientPair.hardwareClient.send("push Yo!");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOTIFICATION_NOT_AUTHORIZED_EXCEPTION)));
    }

    @Test
    public void addPushTokenWorksForAndroid() throws Exception {
        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);

        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        assertNotNull(notification);
        assertEquals(2, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        assertTrue(notification.androidTokens.containsKey("uid1"));
        assertTrue(notification.androidTokens.containsValue("token1"));
    }

    @Test
    public void addPushTokenWorksForIos() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("login " + DEFAULT_TEST_USER +" 1 iOS" + "\0" + "1.10.2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.send("addPushToken 1\0uid2\0token2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(appClient.getBody(), 1);

        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        assertNotNull(notification);
        assertEquals(1, notification.androidTokens.size());
        assertEquals(1, notification.iOSTokens.size());
        Map.Entry<String, String> entry = notification.iOSTokens.entrySet().iterator().next();
        assertEquals("uid2", entry.getKey());
        assertEquals("token2", entry.getValue());
    }

}
