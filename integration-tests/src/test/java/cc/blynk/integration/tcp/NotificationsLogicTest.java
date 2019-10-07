package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.enums.Priority;
import cc.blynk.utils.AppNameUtil;
import io.netty.channel.ChannelFuture;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.deviceOffline;
import static cc.blynk.integration.TestUtil.hardwareConnected;
import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.parseProfile;
import static cc.blynk.integration.TestUtil.readTestUserProfile;
import static cc.blynk.integration.TestUtil.sleep;
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
public class NotificationsLogicTest extends SingleServerInstancePerTest {

    private static int tcpHardPort;

    @BeforeClass
    public static void initPort() {
        tcpHardPort = properties.getHttpPort();
    }

    @Test
    public void addPushTokenWrongInput()  throws Exception  {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        appClient.register("test@test.com", "1", AppNameUtil.BLYNK);
        appClient.verifyResult(ok(1));

        appClient.login("test@test.com", "1", "Android", "RC13");
        appClient.verifyResult(ok(2));

        appClient.createDash("{\"id\":1, \"createdAt\":1, \"name\":\"test board\"}");
        appClient.verifyResult(ok(3));

        appClient.send("addPushToken 1\0uid\0token");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(4)));
    }

    @Test
    public void addPushTokenWorksForAndroid() throws Exception {
        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);

        Notification notification = profile.getDashById(1).getNotificationWidget();
        assertNotNull(notification);
        assertEquals(2, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        assertTrue(notification.androidTokens.containsKey("uid1"));
        assertTrue(notification.androidTokens.containsValue("token1"));
    }

    @Test
    public void addPushTokenNotOverridedOnProfileSave() throws Exception {
        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);

        Notification notification = profile.getDashById(1).getNotificationWidget();
        assertNotNull(notification);
        assertEquals(2, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        assertTrue(notification.androidTokens.containsKey("uid1"));
        assertTrue(notification.androidTokens.containsValue("token1"));

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(4);

        notification = profile.getDashById(1).getNotificationWidget();
        assertNotNull(notification);
        assertEquals(2, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        assertTrue(notification.androidTokens.containsKey("uid1"));
        assertTrue(notification.androidTokens.containsValue("token1"));
    }

    @Test
    public void addPushTokenWorksForIos() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        appClient.login(getUserName(), "1", "iOS", "1.10.2");
        appClient.verifyResult(ok(1));

        appClient.send("addPushToken 1\0uid2\0token2");
        appClient.verifyResult(ok(2));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(1);

        Notification notification = profile.getDashById(1).getNotificationWidget();
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
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = false;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.stop();
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
    }

    @Test
    public void testHardwareDeviceWentOfflineForSecondDeviceSameToken() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = false;
        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.reset();

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.login(clientPair.token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        newHardClient.stop();
        verify(clientPair.appClient.responseMock, timeout(1500)).channelRead(any(), eq(deviceOffline(0, "1-0")));
    }

    @Test
    public void testHardwareDeviceWentOfflineForSecondDeviceNewToken() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = false;
        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.reset();

        Device device1 = new Device(1, "Name", BoardType.ESP8266);

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.getDevice(1, 1);
        device = clientPair.appClient.parseDevice(2);

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(hardwareConnected(1, "1-" + device.id));

        newHardClient.stop();
        clientPair.appClient.verifyResult(deviceOffline(0, "1-" + device.id));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushWorks() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testNotifWidgetOverrideProjectSetting() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        DashBoard dashBoard = profile.getDashById(1);
        dashBoard.isNotificationsOff = true;

        Notification notification = dashBoard.getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(dashBoard);
        clientPair.appClient.verifyResult(ok(1));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline.", 1).toJson();
        assertEquals(expectedJson, message.toJson());

        clientPair.appClient.never(deviceOffline(0, "1-0"));
    }

    @Test
    public void testNoOfflineNotifsExpected() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        DashBoard dashBoard = profile.getDashById(1);
        dashBoard.isNotificationsOff = true;

        Notification notification = dashBoard.getNotificationWidget();
        notification.notifyWhenOffline = false;

        clientPair.appClient.updateDash(dashBoard);
        clientPair.appClient.verifyResult(ok(1));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(holder.gcmWrapper, after(500).never()).send(any(), any(), any());
        clientPair.appClient.never(deviceOffline(0, "1-0"));
    }

    @Test
    public void testOfflineNotifsExpectedButNotPush() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        DashBoard dashBoard = profile.getDashById(1);
        dashBoard.isNotificationsOff = false;

        Notification notification = dashBoard.getNotificationWidget();
        notification.notifyWhenOffline = false;

        clientPair.appClient.updateDash(dashBoard);
        clientPair.appClient.verifyResult(ok(1));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(holder.gcmWrapper, after(500).never()).send(any(), any(), any());
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUser() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.send("logout");
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(ok(2));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(holder.gcmWrapper, after(500).never()).send(any(), any(), any());

        clientPair.appClient.send("logout");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(ok(3)));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUserWithUID() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.send("logout uid");
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(ok(2));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(holder.gcmWrapper, after(500).never()).send(any(), any(), any());

        clientPair.appClient.send("logout");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(ok(3)));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUserWithWrongUID() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.send("logout uidxxx");
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(ok(2));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(holder.gcmWrapper, timeout(500)).send(any(), any(), eq("uid"));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushNotWorksForLogoutUser2() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.getDevice(1, 0);
        Device device = clientPair.appClient.parseDevice(2);

        clientPair.appClient.send("logout");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.stop().await();

        verify(holder.gcmWrapper, after(500).never()).send(any(), any(), any());

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1", "Android", "1.10.4");
        appClient.verifyResult(ok(1));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.login(device.token);
        hardClient.verifyResult(ok(1));

        appClient.send("addPushToken 1\0uid\0token");
        appClient.verifyResult(ok(2));

        hardClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), eq("uid"));
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testLoginWith2AppsAndLogoutFrom1() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1", "Android", "1.10.4");
        appClient.verifyResult(ok(1));

        appClient.send("addPushToken 1\0uid2\0token2");
        appClient.verifyResult(ok(2));

        clientPair.appClient.send("logout uid");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.hardwareClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), eq("uid2"));
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token2", Priority.normal, "Your My Device went offline.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testLoginWith2AppsAndLogoutFrom2() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.getDevice(1, 0);
        Device device = clientPair.appClient.parseDevice(2);

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1", "Android", "1.10.4");
        appClient.verifyResult(ok(1));

        appClient.send("addPushToken 1\0uid2\0token2");
        appClient.verifyResult(ok(2));

        clientPair.appClient.send("logout");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, after(500).never()).send(objectArgumentCaptor.capture(), any(), eq("uid2"));
    }

    @Test
    public void testLoginWithSharedAppAndLogoutFrom() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.send("shareLogin " + getUserName() + " " + token + " Android 24");

        appClient.send("addPushToken 1\0uid2\0token2");
        appClient.verifyResult(ok(2));

        appClient.send("logout uid2");
        appClient.verifyResult(ok(3));

        clientPair.hardwareClient.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, after(500).never()).send(objectArgumentCaptor.capture(), any(), eq("uid2"));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushDelayedWorks() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;
        notification.notifyWhenOfflineIgnorePeriod = 1000;

        long now = System.currentTimeMillis();

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.stop();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);

        verify(holder.gcmWrapper, timeout(2000).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();
        assertTrue(System.currentTimeMillis() - now > notification.notifyWhenOfflineIgnorePeriod );

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
    }

    @Test
    public void testHardwareDeviceWentOfflineAndPushDelayedNotTriggeredDueToReconnect() throws Exception {
        Profile profile = parseProfile(readTestUserProfile());
        Notification notification = profile.getDashById(1).getNotificationWidget();
        notification.notifyWhenOffline = true;
        notification.notifyWhenOfflineIgnorePeriod = 1000;

        clientPair.appClient.updateDash(profile.getDashById(1));
        clientPair.appClient.verifyResult(ok(1));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        clientPair.appClient.getDevice(1, 0);
        Device device = clientPair.appClient.parseDevice(2);

        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.login(device.token);
        newHardClient.verifyResult(ok(1));

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, after(1500).never()).send(objectArgumentCaptor.capture(), any(), any());
    }

    @Test
    public void testCreateNewNotificationWidget() throws Exception  {
        clientPair.appClient.deleteWidget(1, 9);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.createWidget(1, "{\"id\":9, \"x\":1, \"y\":1, \"width\":1, \"height\":1, \"type\":\"NOTIFICATION\", \"notifyWhenOfflineIgnorePeriod\":0, \"priority\":\"high\", \"notifyWhenOffline\":true}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.updateWidget(1, "{\"id\":9, \"x\":1, \"y\":1, \"width\":1, \"height\":1, \"type\":\"NOTIFICATION\", \"notifyWhenOfflineIgnorePeriod\":0, \"priority\":\"high\", \"notifyWhenOffline\":false}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.hardwareClient.send("push 123");

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);

        verify(holder.gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token1", Priority.high, "123", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testPushWhenHardwareOffline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(750).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testPushHandler() throws Exception {
        clientPair.hardwareClient.send("push Yo!");

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Yo!", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testPushHandlerWithPlaceHolder() throws Exception {
        clientPair.hardwareClient.send("push Yo {DEVICE_NAME}!");

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Yo My Device!", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testOfflineMessageIsSentToBothApps()  throws Exception  {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "iOS", "1.10.2");
        appClient.verifyResult(ok(1));

        clientPair.appClient.deleteWidget(1, 9);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.stop();
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
        appClient.verifyResult(deviceOffline(0, "1-0"));
    }

    @Test
    public void multipleAccountsOnTheSameDevice() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "iOS", "1.10.2");
        appClient.verifyResult(ok(1));

        appClient.send("addPushToken 1\0uid\0token");
        appClient.verifyResult(ok(2));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(3);

        Notification notification = profile.getDashById(1).getNotificationWidget();
        assertNotNull(notification);
        assertEquals(1, notification.androidTokens.size());
        assertEquals(1, notification.iOSTokens.size());

        appClient.reset();

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("testuser@test.com", "1", AppNameUtil.BLYNK);
        appClient2.verifyResult(ok(1));

        appClient2.login("testuser@test.com", "1", "iOS", "1.10.2");
        appClient2.verifyResult(ok(2));

        DashBoard dash = new DashBoard();
        dash.id = 5;
        dash.name = "test";
        Notification notif = new Notification();
        notif.x = 1;
        notif.y = 1;
        notif.width = 1;
        notif.height = 1;
        notif.id = 22;
        dash.widgets = new Widget[] {
                notif
        };
        dash.activate();
        appClient2.createDash(dash);
        appClient2.verifyResult(ok(3));

        appClient2.send("addPushToken 5\0uid\0token222");
        appClient2.verifyResult(ok(4));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(5);

        notification = profile.getDashById(5).getNotificationWidget();
        assertNotNull(notification);
        assertEquals(0, notification.androidTokens.size());
        assertEquals(1, notification.iOSTokens.size());
        appClient2.reset();

        //waiting for another thread to remove the duplicate
        sleep(500);

        appClient.send("loadProfileGzipped");
        profile = appClient.parseProfile(1);
        notification = profile.getDashById(1).getNotificationWidget();
        assertNotNull(notification);
        assertEquals(1, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(1);
        notification = profile.getDashById(5).getNotificationWidget();
        assertNotNull(notification);
        assertEquals(0, notification.androidTokens.size());
        assertEquals(1, notification.iOSTokens.size());
    }


}
