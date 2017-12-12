package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.model.messages.appllication.CreateDevice;
import cc.blynk.server.core.protocol.model.messages.appllication.DeviceOfflineMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareConnectedMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.enums.Priority;
import io.netty.channel.ChannelFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationsLogicTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();

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
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("login test@test.com 1 Android" + "\0" + "RC13");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.send("createDash {\"id\":1, \"createdAt\":1, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        appClient.send("addPushToken 1\0uid\0token");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(4)));
    }

    @Test
    public void addPushTokenWorksForAndroid() throws Exception {
        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody(2));

        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        assertNotNull(notification);
        assertEquals(2, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        assertTrue(notification.androidTokens.containsKey("uid1"));
        assertTrue(notification.androidTokens.containsValue("token1"));
    }

    @Test
    public void addPushTokenNotOverridedOnProfileSave() throws Exception {
        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody(2));

        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        assertNotNull(notification);
        assertEquals(2, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        assertTrue(notification.androidTokens.containsKey("uid1"));
        assertTrue(notification.androidTokens.containsValue("token1"));

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody(4));

        notification = profile.getDashById(1).getWidgetByType(Notification.class);
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
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("addPushToken 1\0uid2\0token2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(appClient.getBody());

        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        assertNotNull(notification);
        assertEquals(1, notification.androidTokens.size());
        assertEquals(1, notification.iOSTokens.size());
        Map.Entry<String, String> entry = notification.iOSTokens.entrySet().iterator().next();
        assertEquals("uid2", entry.getKey());
        assertEquals("token2", entry.getValue());
    }

    @Test
    public void testHardwareDeviceWentOffline() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = false;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.stop();
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new DeviceOfflineMessage(0, "1-0")));
    }

    @Test
    public void testHardwareDeviceWentOfflineForSecondDeviceSameToken() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = false;
        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.send("login " + token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        newHardClient.stop();
        verify(clientPair.appClient.responseMock, timeout(1500)).channelRead(any(), eq(new DeviceOfflineMessage(0, "1-0")));
    }

    @Test
    public void testHardwareDeviceWentOfflineForSecondDeviceNewToken() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = false;
        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        Device device1 = new Device(1, "Name", "ESP8266");

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.send("getToken 1-1");
        String token = clientPair.appClient.getBody(2);

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.send("login " + token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareConnectedMessage(1, "1-1")));

        newHardClient.stop();
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new DeviceOfflineMessage(0, "1-1")));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushWorks() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline. \"My Dashboard\" project is disconnected.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUser() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        clientPair.appClient.send("logout");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(gcmWrapper, after(500).never()).send(any(), any(), any());

        clientPair.appClient.send("logout");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(ok(3)));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUserWithUID() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        clientPair.appClient.send("logout uid");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(gcmWrapper, after(500).never()).send(any(), any(), any());

        clientPair.appClient.send("logout");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(ok(3)));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUserWithWrongUID() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        clientPair.appClient.send("logout uidxxx");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(gcmWrapper, timeout(500)).send(any(), any(), eq("uid"));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUser2() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody(2);

        clientPair.appClient.send("logout");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.hardwareClient.stop().await();

        verify(gcmWrapper, after(500).never()).send(any(), any(), any());

        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);
        appClient.start();
        appClient.send("login dima@mail.ua 1 Android" + "\0" + "1.10.4");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.send("login " + token);
        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("addPushToken 1\0uid\0token");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        hardClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), eq("uid"));
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline. \"My Dashboard\" project is disconnected.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testLoginWith2AppsAndLogoutFrom1() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody(2);

        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);
        appClient.start();
        appClient.send("login dima@mail.ua 1 Android" + "\0" + "1.10.4");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("addPushToken 1\0uid2\0token2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("logout uid");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.hardwareClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), eq("uid2"));
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token2", Priority.normal, "Your My Device went offline. \"My Dashboard\" project is disconnected.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testLoginWith2AppsAndLogoutFrom2() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody(2);

        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);
        appClient.start();
        appClient.send("login dima@mail.ua 1 Android" + "\0" + "1.10.4");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("addPushToken 1\0uid2\0token2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("logout");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.hardwareClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, after(500).never()).send(objectArgumentCaptor.capture(), any(), eq("uid2"));
    }

    @Test
    public void testLoginWithSharedAppAndLogoutFrom() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();

        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);
        appClient.start();
        appClient.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        appClient.send("addPushToken 1\0uid2\0token2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.send("logout uid2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.hardwareClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, after(500).never()).send(objectArgumentCaptor.capture(), any(), eq("uid2"));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushDelayedWorks() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;
        notification.notifyWhenOfflineIgnorePeriod = 1000;

        long now = System.currentTimeMillis();

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.stop();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);

        verify(gcmWrapper, timeout(2000).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();
        assertTrue(System.currentTimeMillis() - now > notification.notifyWhenOfflineIgnorePeriod );

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline. \"My Dashboard\" project is disconnected.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushDelayedNotTriggeredDueToReconnect() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = true;
        notification.notifyWhenOfflineIgnorePeriod = 1000;

        clientPair.appClient.send("updateDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();


        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody(2);

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.send("login " + token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, after(1500).never()).send(objectArgumentCaptor.capture(), any(), any());
    }

    @Test
    public void testCreateNewNotificationWidget() throws Exception  {
        clientPair.appClient.send("deleteWidget 1\0" + "9");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("createWidget 1\0{\"id\":9, \"x\":1, \"y\":1, \"width\":1, \"height\":1, \"type\":\"NOTIFICATION\", \"notifyWhenOfflineIgnorePeriod\":0, \"priority\":\"high\", \"notifyWhenOffline\":true}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("updateWidget 1\0{\"id\":9, \"x\":1, \"y\":1, \"width\":1, \"height\":1, \"type\":\"NOTIFICATION\", \"notifyWhenOfflineIgnorePeriod\":0, \"priority\":\"high\", \"notifyWhenOffline\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.hardwareClient.send("push 123");

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);

        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token1", Priority.high, "123", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testPushWhenHardwareOffline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline. \"My Dashboard\" project is disconnected.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testPushHandler() throws Exception {
        clientPair.hardwareClient.send("push Yo!");

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Yo!", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testOfflineMessageIsSentToBothApps()  throws Exception  {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);
        appClient.start();

        appClient.send("login " + DEFAULT_TEST_USER +" 1 iOS" + "\0" + "1.10.2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("deleteWidget 1" + "\0" + "9");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.stop();
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new DeviceOfflineMessage(0, "1-0")));
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new DeviceOfflineMessage(0, "1-0")));
    }


}
