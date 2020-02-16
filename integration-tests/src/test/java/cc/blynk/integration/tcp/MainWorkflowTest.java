package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DashboardSettings;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.serialization.View;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.Step;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.others.Player;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.TimeInput;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.notifications.mail.QrHolder;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.SHA256Util;
import io.netty.channel.ChannelFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;

import static cc.blynk.integration.TestUtil.appIsOutdated;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.deviceOffline;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.hardwareConnected;
import static cc.blynk.integration.TestUtil.illegalCommand;
import static cc.blynk.integration.TestUtil.illegalCommandBody;
import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.parseProfile;
import static cc.blynk.integration.TestUtil.readTestUserProfile;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_INVALID_BODY;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_NOT_AUTHORIZED;
import static cc.blynk.server.core.protocol.enums.Response.NO_ACTIVE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Response.QUOTA_LIMIT;
import static cc.blynk.server.core.protocol.enums.Response.USER_ALREADY_REGISTERED;
import static cc.blynk.server.core.protocol.enums.Response.USER_NOT_AUTHENTICATED;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MainWorkflowTest extends SingleServerInstancePerTest {

    @Test
    public void testCloneForLocalServerWithNoDB() throws Exception  {
        assertFalse(holder.dbManager.isDBEnabled());

        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getProjectByCloneCode " + token);
        DashBoard dashBoard = clientPair.appClient.parseDash(2);
        assertEquals("My Dashboard", dashBoard.name);
    }

    @Test
    public void testResetEmail() throws Exception {
        String userName = getUserName();
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.send("resetPass start " + userName + " " + AppNameUtil.BLYNK);
        appClient.verifyResult(ok(1));

        appClient.send("resetPass start " + userName + " " + AppNameUtil.BLYNK);
        appClient.verifyResult(notAllowed(2));

        String token = holder.tokensPool.getTokens().entrySet().iterator().next().getKey();
        verify(holder.mailWrapper).sendWithAttachment(eq(userName), eq("Password restoration for your Blynk account."), contains("http://blynk-cloud.com/restore?token=" + token), any(QrHolder.class));

        appClient.send("resetPass verify 123");
        appClient.verifyResult(notAllowed(3));

        appClient.send("resetPass verify " + token);
        appClient.verifyResult(ok(4));

        appClient.send("resetPass reset " + token + " " + SHA256Util.makeHash("2", userName));
        appClient.verifyResult(ok(5));
        //verify(holder.mailWrapper).sendHtml(eq(userName), eq("Your new password on Blynk"), contains("You have changed your password on Blynk. Please, keep it in your records so you don't forget it."));

        appClient.login(userName, "1");
        appClient.verifyResult(new ResponseMessage(6, USER_NOT_AUTHENTICATED));

        appClient.login(userName, "2");
        appClient.verifyResult(ok(7));
    }

    @Test
    public void testResetEmail2() throws Exception {
        String userName = getUserName();
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.send("resetPass start " + userName + " " + AppNameUtil.BLYNK);
        appClient.verifyResult(ok(1));

        appClient.send("resetPass start " + userName + " " + AppNameUtil.BLYNK);
        appClient.verifyResult(notAllowed(2));

        String token = holder.tokensPool.getTokens().entrySet().iterator().next().getKey();
        verify(holder.mailWrapper).sendWithAttachment(eq(userName), eq("Password restoration for your Blynk account."), contains("http://blynk-cloud.com/restore?token=" + token), any(QrHolder.class));

        appClient.send("resetPass verify 123");
        appClient.verifyResult(notAllowed(3));

        appClient.send("resetPass verify " + token);
        appClient.verifyResult(ok(4));

        appClient.send("resetPass reset " + token + " " + SHA256Util.makeHash("2", userName));
        appClient.verifyResult(ok(5));
        //verify(holder.mailWrapper).sendHtml(eq(userName), eq("Your new password on Blynk"), contains("You have changed your password on Blynk. Please, keep it in your records so you don't forget it."));

        appClient.login(userName, "1");
        appClient.verifyResult(new ResponseMessage(6, USER_NOT_AUTHENTICATED));

        appClient.login(userName, "2");
        appClient.verifyResult(ok(7));
    }

    @Test
    public void registrationAllowedOnlyOncePerConnection() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        appClient.register("test1@test.com", "1", AppNameUtil.BLYNK);
        appClient.verifyResult(ok(1));

        appClient.register("test2@test.com", "1", AppNameUtil.BLYNK);
        appClient.verifyResult(notAllowed(2));

        assertTrue(appClient.isClosed());
    }

    @Test
    public void createBasicProfile() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        String username = incrementAndGetUserName();

        appClient.register(username, "1", AppNameUtil.BLYNK);
        appClient.verifyResult(ok(1));

        appClient.login(username, "1", "Android", "RC13");
        appClient.verifyResult(ok(2));

        appClient.createDash("{\"id\":1, \"createdAt\":1, \"name\":\"test board\"}");
        appClient.verifyResult(ok(3));

        appClient.createWidget(1, "{\"id\":1, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":1}");
        appClient.verifyResult(ok(4));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(1);
        profile.dashBoards[0].updatedAt = 0;

        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", profile.toString());

        appClient.createWidget(1, "{\"id\":2, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        appClient.verifyResult(ok(2));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = appClient.parseProfile(1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"pushMode\":false},{\"type\":\"BUTTON\",\"id\":2,\"x\":2,\"y\":2,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text 2\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":2,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", profile.toString());

        appClient.updateWidget(1, "{\"id\":2, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"new label\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":3}\"");
        appClient.verifyResult(ok(2));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = appClient.parseProfile(1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"pushMode\":false},{\"type\":\"BUTTON\",\"id\":2,\"x\":2,\"y\":2,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"new label\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":3,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", profile.toString());

        appClient.deleteWidget(1, 3);
        appClient.verifyResult(illegalCommand(2));

        appClient.deleteWidget(1, 1);
        appClient.verifyResult(ok(3));

        appClient.deleteWidget(1, 2);
        appClient.verifyResult(ok(4));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = appClient.parseProfile(1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", profile.toString());
    }

    @Test
    public void testNoEmptyPMCommands() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        String username = incrementAndGetUserName();

        appClient.register(username, "1", AppNameUtil.BLYNK);
        appClient.verifyResult(ok(1));

        appClient.login(username, "1", "Android", "RC13");
        appClient.verifyResult(ok(2));

        appClient.createDash("{\"id\":1, \"createdAt\":1, \"name\":\"test board\"}");
        appClient.verifyResult(ok(3));

        appClient.createWidget(1, "{\"id\":1, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":1}");
        appClient.verifyResult(ok(4));

        Device device = new Device();
        device.id = 1;
        device.name = "123";
        device.boardType = BoardType.ESP32_Dev_Board;
        appClient.createDevice(1, device);
        device = appClient.parseDevice(5);

        assertNotNull(device);
        assertNotNull(device.token);
        appClient.verifyResult(createDevice(5, device));

        appClient.activate(1);
        appClient.verifyResult(new ResponseMessage(6, DEVICE_NOT_IN_NETWORK));

        TestHardClient hardClient = new TestHardClient("localhost", properties.getHttpPort());
        hardClient.start();

        hardClient.login(device.token);
        hardClient.verifyResult(ok(1));
        hardClient.never(hardware(1, "pm"));
        appClient.verifyResult(new StringMessage(1, HARDWARE_CONNECTED, "1-1"));

        appClient.activate(1);
        appClient.verifyResult(ok(7));
        hardClient.never(hardware(1, "pm"));
    }

    @Test
    public void doNotAllowUsersWithQuestionMark() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        appClient.register("te?st@test.com", "1", AppNameUtil.BLYNK);
        appClient.verifyResult(illegalCommand(1));
    }

    @Test
    public void createDashWithDevices() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        appClient.register("test@test.com", "1", AppNameUtil.BLYNK);
        appClient.verifyResult(ok(1));

        appClient.login("test@test.com", "1", "Android", "RC13");
        appClient.verifyResult(ok(2));

        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "AAAa";
        Device device = new Device();
        device.id = 0;
        device.name = "123";
        dash.devices = new Device[] {device};

        appClient.createDash("no_token\0" + dash.toString());
        appClient.verifyResult(ok(3));

        appClient.send("getDevices 1");

        Device[] devices = appClient.parseDevices(4);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        assertEquals(0, devices[0].id);
        assertEquals("123", devices[0].name);
        assertNull(devices[0].token);
    }

    @Test
    public void testRegisterWithAnotherApp() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        appClient.register(getUserName(), "1", "MyApp");
        appClient.verifyResult(ok(1));

        appClient.login(getUserName(), "1", "Android", "1.13.3", "MyApp");
        appClient.verifyResult(ok(2));

        appClient.createDash("{\"id\":1, \"createdAt\":1, \"name\":\"test board\"}");
        appClient.verifyResult(ok(3));

        appClient.createWidget(1, "{\"id\":1, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":1}");
        appClient.verifyResult(ok(4));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(1);
        profile.dashBoards[0].updatedAt = 0;

        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", profile.toString());
    }

    @Test
    public void testDoubleLogin() throws Exception {
        clientPair.hardwareClient.login(getUserName() + " 1");
        clientPair.hardwareClient.verifyResult(new ResponseMessage(1, USER_ALREADY_REGISTERED));
    }

    @Test
    public void testDoubleLogin2() throws Exception {
        TestHardClient newHardwareClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardwareClient.start();
        newHardwareClient.login(clientPair.token);
        newHardwareClient.login(clientPair.token);
        newHardwareClient.verifyResult(ok(1));
        newHardwareClient.verifyResult(new ResponseMessage(2, USER_ALREADY_REGISTERED));
    }

    @Test
    public void sendCommandBeforeLogin() {
        TestHardClient newHardwareClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardwareClient.start();
        newHardwareClient.send("hardware vw 1 1");

        long tries = 0;
        while(!newHardwareClient.isClosed() && tries < 10) {
            TestUtil.sleep(100);
            tries++;
        }
        assertTrue(newHardwareClient.isClosed());
    }

    @Test
    public void testForwardBluetoothFromAppWorks() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":743, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"STEP\", \"pwmMode\":true, \"pinType\":\"VIRTUAL\", \"pin\":67}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardwareBT 1-0 vw 67 100");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(hardware(2, "1-0 vw 67 100")));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);
        assertNotNull(profile);
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (short) 67, PinType.VIRTUAL);
        assertNotNull(widget);
        assertTrue(widget instanceof Step);
        assertEquals("100", ((OnePinWidget) widget).value);
    }

    @Test
    public void testValueForPWMPinForStteperIsAccepted() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":743, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"STEP\", \"pwmMode\":true, \"pinType\":\"DIGITAL\", \"pin\":24}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 aw 24 100");
        clientPair.hardwareClient.verifyResult(hardware(2, "aw 24 100"));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);
        assertNotNull(profile);
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (short) 24, PinType.DIGITAL);
        assertNotNull(widget);
        assertTrue(widget instanceof Step);
        assertEquals("100", ((OnePinWidget) widget).value);
    }

    @Test
    public void testSendInvalidVirtualPin() throws Exception {
        clientPair.hardwareClient.send("hardware vw 256 100");
        clientPair.hardwareClient.verifyResult(illegalCommand(1));
    }

    @Test
    public void testSendInvalidVirtualPin2() throws Exception {
        clientPair.hardwareClient.send("hardware vw -1 100");
        clientPair.hardwareClient.verifyResult(illegalCommand(1));
    }

    @Test
    public void testSendValidVirtualPin() throws Exception {
        clientPair.hardwareClient.send("hardware vw 0 100");
        clientPair.hardwareClient.send("hardware vw 255 100");
        clientPair.hardwareClient.never(illegalCommand(1));
        clientPair.hardwareClient.never(illegalCommand(2));
    }

    @Test
    public void testNoEnergyDrainForBusinessApps() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);

        appClient.start();

        appClient.register("test@test.com", "1", "MyApp");
        appClient.verifyResult(ok(1));

        appClient.login("test@test.com", "1", "Android", "1.13.3", "MyApp");
        appClient.verifyResult(ok(2));

        appClient.createDash("{\"id\":2, \"createdAt\":1458856800001, \"name\":\"test board\"}");
        appClient.verifyResult(ok(3));

        appClient.send("getEnergy");
        appClient.verifyResult(produce(4, GET_ENERGY, "2000"));

        appClient.createWidget(2, "{\"id\":2, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        appClient.verifyResult(ok(5));

        appClient.createWidget(2, "{\"id\":3, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        appClient.verifyResult(ok(6));

        appClient.createWidget(2, "{\"id\":4, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        appClient.verifyResult(ok(7));

        appClient.createWidget(2, "{\"id\":5, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        appClient.verifyResult(ok(8));

        appClient.createWidget(2, "{\"id\":6, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        appClient.verifyResult(ok(9));

        appClient.createWidget(2, "{\"id\":7, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        appClient.verifyResult(ok(10));
    }

    @Test
    public void testPingCommandWorks() throws Exception {
        clientPair.appClient.send("ping");
        clientPair.appClient.verifyResult(ok(1));
    }

    @Test
    public void testAddAndRemoveTabs() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(2, GET_ENERGY, "7500"));

        clientPair.appClient.createWidget(1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}, {\"label\":\"tab 3\"}], \"type\":\"TABS\"}");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.createWidget(1, "{\"id\":101, \"width\":1, \"height\":1, \"x\":15, \"y\":0, \"tabId\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":18}");
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.createWidget(1, "{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":17}");
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(6, GET_ENERGY, "7100"));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        assertEquals(19, profile.dashBoards[0].widgets.length);

        clientPair.appClient.deleteWidget(1, 100);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(3, GET_ENERGY, "7300"));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        assertEquals(17, profile.dashBoards[0].widgets.length);
        assertNotNull(profile.dashBoards[0].findWidgetByPin(0, (short) 17, PinType.DIGITAL));
    }

    @Test
    public void testAddAndUpdateTabs() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(2, GET_ENERGY, "7500"));

        clientPair.appClient.createWidget(1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}, {\"label\":\"tab 3\"}], \"type\":\"TABS\"}");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.createWidget(1, "{\"id\":101, \"width\":1, \"height\":1, \"x\":15, \"y\":0, \"tabId\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":18}");
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.createWidget(1, "{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":2, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":17}");
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(6, GET_ENERGY, "7100"));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        assertEquals(19, profile.dashBoards[0].widgets.length);

        clientPair.appClient.updateWidget(1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}], \"type\":\"TABS\"}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(3, GET_ENERGY, "7300"));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        assertEquals(18, profile.dashBoards[0].widgets.length);
        assertNull(profile.dashBoards[0].findWidgetByPin(0, (short) 17, PinType.DIGITAL));
        assertNotNull(profile.dashBoards[0].findWidgetByPin(0, (short) 18, PinType.DIGITAL));
    }

    @Test
    public void testPurchaseEnergy() throws Exception {
        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "5262996016779471529.4493624392154338");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(1)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "A3B93EE9-BC65-499E-A660-F2A84F2AF1FC");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "com.blynk.energy.280001461578468247");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(3)));

        clientPair.appClient.send("addEnergy " + "1000");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(4)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(5)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "150000195113772");
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "1370-3990-1414-55681");
        clientPair.appClient.verifyResult(ok(7));
    }

    @Test
    public void testApplicationPingCommandOk() throws Exception {
        clientPair.appClient.send("ping");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();

        clientPair.appClient.send("ping");
        clientPair.appClient.verifyResult(ok(1));
    }

    @Test
    public void testHardPingCommandOk() throws Exception {
        clientPair.hardwareClient.send("ping");
        clientPair.hardwareClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("ping");
        clientPair.hardwareClient.verifyResult(ok(2));
    }

    @Test
    public void testDashCommands() throws Exception {
        clientPair.appClient.updateDash("{\"id\":10, \"name\":\"test board update\"}");
        clientPair.appClient.verifyResult(illegalCommand(1));

        clientPair.appClient.createDash("{\"id\":10, \"name\":\"test board\"}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.createDash("{\"id\":10, \"name\":\"test board\"}");
        clientPair.appClient.verifyResult(notAllowed(3));

        clientPair.appClient.updateDash("{\"id\":10, \"name\":\"test board update\"}");
        clientPair.appClient.verifyResult(ok(4));

        clientPair.hardwareClient.send("ping");

        clientPair.appClient.deleteDash(1);
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.deleteDash(1);
        clientPair.appClient.verifyResult(illegalCommand(6));
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
        clientPair.appClient.reset();

        Profile responseProfile;
        DashBoard responseDash;

        clientPair.appClient.send("loadProfileGzipped");
        responseProfile = clientPair.appClient.parseProfile(1);
        responseProfile.dashBoards[0].updatedAt = 0;
        responseProfile.dashBoards[0].createdAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}", responseProfile.toString());

        clientPair.appClient.send("loadProfileGzipped 10");
        responseDash = clientPair.appClient.parseDash(2);
        responseDash.updatedAt = 0;
        responseDash.createdAt = 0;
        assertEquals("{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}", responseDash.toString());

        clientPair.appClient.send("loadProfileGzipped 1");
        clientPair.appClient.verifyResult(illegalCommand(3));

        clientPair.appClient.activate(10);
        clientPair.appClient.verifyResult(new ResponseMessage(4, DEVICE_NOT_IN_NETWORK));

        clientPair.appClient.send("loadProfileGzipped");
        responseProfile = clientPair.appClient.parseProfile(5);
        responseProfile.dashBoards[0].updatedAt = 0;
        responseProfile.dashBoards[0].createdAt = 0;
        String expectedProfile = "{\"dashBoards\":[{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":true,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}";
        assertEquals(expectedProfile, responseProfile.toString());

        clientPair.appClient.updateDash("{\"id\":10,\"name\":\"test board update\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}");
        clientPair.appClient.verifyResult(ok(6));

        expectedProfile = "{\"dashBoards\":[{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":true,\"widgetBackgroundOn\":false,\"color\":-1,\"isDefaultColor\":true}]}";
        clientPair.appClient.send("loadProfileGzipped");
        responseProfile = clientPair.appClient.parseProfile(7);
        responseProfile.dashBoards[0].updatedAt = 0;
        responseProfile.dashBoards[0].createdAt = 0;
        assertEquals(expectedProfile, responseProfile.toString());
    }

    @Test
    public void testHardwareChannelClosedOnDashRemoval() throws Exception {
        String username = getUserName();
        String tempDir = holder.props.getProperty("data.folder");
        Path userReportFolder = Paths.get(tempDir, "data", username);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath10 = Paths.get(tempDir, "data", username,
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.MINUTE));
        Path pinReportingDataPath11 = Paths.get(tempDir, "data", username,
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.HOURLY));
        Path pinReportingDataPath12 = Paths.get(tempDir, "data", username,
                ReportingDiskDao.generateFilename(1, 0, PinType.DIGITAL, (short) 8, GraphGranularityType.DAILY));
        Path pinReportingDataPath13 = Paths.get(tempDir, "data", username,
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 9, GraphGranularityType.DAILY));

        FileUtils.write(pinReportingDataPath10, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath11, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath12, 1.11D, 1111111);
        FileUtils.write(pinReportingDataPath13, 1.11D, 1111111);

        clientPair.appClient.deleteDash(1);
        clientPair.appClient.verifyResult(ok(1));

        assertTrue(clientPair.hardwareClient.isClosed());
        assertTrue(Files.notExists(pinReportingDataPath10));
        assertTrue(Files.notExists(pinReportingDataPath11));
        assertTrue(Files.notExists(pinReportingDataPath12));
        assertTrue(Files.notExists(pinReportingDataPath13));
    }

    @Test
    public void testGetTokenWorksWithNewFormats() throws Exception {
        clientPair.appClient.createDash("{\"id\":10, \"name\":\"test board\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.createDevice(10, new Device(2, "123", BoardType.ESP8266));
        Device device = clientPair.appClient.parseDevice(2);
        String token = device.token;
        assertNotNull(token);

        clientPair.appClient.getDevice(10, 2);
        Device device2 = clientPair.appClient.parseDevice(3);
        assertNotNull(device2);
        assertEquals(token, device2.token);

        clientPair.appClient.getDevice(10, 2);
        device2 = clientPair.appClient.parseDevice(4);
        assertNotNull(device2);
        assertEquals(token, device2.token);

        clientPair.appClient.createDash("{\"id\":11, \"name\":\"test board\"}");
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.getDevice(11, 0);
        clientPair.appClient.verifyResult(illegalCommandBody(6));
    }

    @Test
    public void deleteDashDeletesTokensAlso() throws Exception {
        clientPair.appClient.createDash("{\"id\":10, \"name\":\"test board\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();
        clientPair.appClient.createDevice(10, new Device(2, "123", BoardType.ESP8266));
        Device device = clientPair.appClient.parseDevice();
        String token = device.token;
        assertNotNull(token);

        clientPair.appClient.reset();
        clientPair.appClient.send("getShareToken 10");
        String sharedToken = clientPair.appClient.getBody();
        assertNotNull(sharedToken);

        clientPair.appClient.deleteDash(10);
        clientPair.appClient.verifyResult(ok(2));

        TestHardClient newHardClient = new TestHardClient("localhost", properties.getHttpPort());
        newHardClient.start();
        newHardClient.login(token);
        newHardClient.verifyResult(new ResponseMessage(1, INVALID_TOKEN));

        TestAppClient newAppClient = new TestAppClient(properties);
        newAppClient.start();
        newAppClient.send("shareLogin " + getUserName() + " " + sharedToken + " Android 24");

        newAppClient.verifyResult(notAllowed(1));
    }

    @Test
    public void loadGzippedProfile() throws Exception{
        Profile expectedProfile = JsonParser.parseProfileFromString(readTestUserProfile());

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

        profile.dashBoards[0].updatedAt = 0;

        expectedProfile.dashBoards[0].devices = null;
        profile.dashBoards[0].devices = null;

        assertEquals(expectedProfile.toString(), profile.toString());
    }

    @Test
    public void settingsUpdateCommand() throws Exception{
        DashboardSettings settings = new DashboardSettings("New Name",
                true, Theme.BlynkLight, true, true, false, false, 0, false);

        clientPair.appClient.send("updateSettings 1\0" + JsonParser.toJson(settings));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);
        DashBoard dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(settings.name, dashBoard.name);
        assertEquals(settings.isAppConnectedOn, dashBoard.isAppConnectedOn);
        assertEquals(settings.isNotificationsOff, dashBoard.isNotificationsOff);
        assertEquals(settings.isShared, dashBoard.isShared);
        assertEquals(settings.keepScreenOn, dashBoard.keepScreenOn);
        assertEquals(settings.theme, dashBoard.theme);
    }

    @Test
    public void testSendUnicodeChar() throws Exception {
        clientPair.hardwareClient.send("hardware vw 1 °F");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "1-0 vw 1 °F")));
    }

    @Test
    public void testAppSendAnyHardCommandAndBack() throws Exception {
        clientPair.appClient.send("hardware 1 dw 1 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "dw 1 1")));

        clientPair.hardwareClient.send("hardware ar 2");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(hardware(2, "ar 2")));
    }

    @Test
    public void testAppNoActiveDashForHard() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "1-0 aw 1 1")));

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));
    }

    @Test
    public void testHardwareSendsWrongCommand() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 ");
        clientPair.hardwareClient.verifyResult(illegalCommand(1));

        clientPair.hardwareClient.send("hardware aw 1");
        clientPair.hardwareClient.verifyResult(illegalCommand(2));
    }

    @Test
    public void testAppChangeActiveDash() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "1-0 aw 1 1")));

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        Profile newProfile = parseProfile(readTestUserProfile("user_profile_json_3_dashes.txt"));
        clientPair.appClient.createDash(newProfile.dashBoards[1]);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.activate(2);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(3, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(4, "1-0 aw 1 1")));
    }

    @Test
    public void testActive2AndDeactivate1() throws Exception {
        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        Profile newProfile = parseProfile(readTestUserProfile("user_profile_json_3_dashes.txt"));
        clientPair.appClient.createDash(newProfile.dashBoards[1]);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.activate(2);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.appClient.reset();

        clientPair.appClient.createDevice(2, new Device(2, "123", BoardType.ESP8266));
        Device device = clientPair.appClient.parseDevice();
        String token2 = device.token;
        hardClient2.login(token2);
        hardClient2.verifyResult(ok(1));

        clientPair.appClient.reset();

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "1-0 aw 1 1")));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(2, "2-" + device.id + " aw 1 1")));


        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(3, "2-" + device.id + " aw 1 1")));
        hardClient2.stop().awaitUninterruptibly();
    }

    @Test
    public void testActivateWorkflow() throws Exception {
        clientPair.appClient.activate(2);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(1)));

        clientPair.appClient.deactivate(2);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(2)));

        clientPair.appClient.send("hardware 1 ar 1 1");
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(ok(3)));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(4));
    }

    @Test
    public void testTweetNotWorks() throws Exception {
        clientPair.hardwareClient.send("tweet");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOTIFICATION_INVALID_BODY)));

        clientPair.hardwareClient.send("tweet ");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOTIFICATION_INVALID_BODY)));

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < 141; i++) {
            a.append("a");
        }

        clientPair.hardwareClient.send("tweet " + a);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NOTIFICATION_INVALID_BODY)));

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, NOTIFICATION_NOT_AUTHORIZED)));
    }

    @Test
    public void testSmsWorks() throws Exception {
        clientPair.hardwareClient.send("sms");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOTIFICATION_INVALID_BODY)));

        //no sms widget
        clientPair.hardwareClient.send("sms yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOTIFICATION_NOT_AUTHORIZED)));

        //adding sms widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"width\":1, \"height\":1, \"to\":\"3809683423423\", \"x\":0, \"y\":0, \"type\":\"SMS\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("sms yo");
        verify(holder.smsWrapper, timeout(500)).send(eq("3809683423423"), eq("yo"));
        clientPair.hardwareClient.verifyResult(ok(3));

        clientPair.hardwareClient.send("sms yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, QUOTA_LIMIT)));
    }

    @Test
    public void testTweetWorks() throws Exception {
        clientPair.hardwareClient.send("tweet yo");
        verify(holder.twitterWrapper, timeout(500)).send(eq("token"), eq("secret"), eq("yo"), any());

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, QUOTA_LIMIT)));
    }

    @Test
    public void testPlayerUpdateWorksAsExpected() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"PLAYER\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1}");

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw 99 play");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 play")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        Player player = (Player) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(player);
        assertTrue(player.isOnPlay);

        clientPair.appClient.send("hardware 1 vw 99 stop");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 stop")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        player = (Player) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(player);
        assertFalse(player.isOnPlay);
    }

    @Test
    public void testTimeInputUpdateWorksAsExpected() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"TIME_INPUT\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1}");

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82860 Europe/Kiev 1"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 82800 82860 Europe/Kiev 1")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        TimeInput timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[] {1}, timeInput.days);


        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82860 Europe/Kiev "));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 82800 82860 Europe/Kiev ")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 82800  Europe/Kiev "));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 82800  Europe/Kiev ")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 82800  Europe/Kiev 1,2,3,4"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 82800  Europe/Kiev 1,2,3,4")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99   Europe/Kiev 1,2,3,4"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99   Europe/Kiev 1,2,3,4")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-1, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82800 Europe/Kiev  10800"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 82800 82800 Europe/Kiev  10800")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82800, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 ss sr Europe/Kiev  10800"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 ss sr Europe/Kiev  10800")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-2, timeInput.startAt);
        assertEquals(-3, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);
    }

    @Test
    public void testTimeInputUpdateWorksAsExpectedFromHardSide() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"TIME_INPUT\",\"orgId\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1}");

        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.reset();

        clientPair.hardwareClient.send("hardware vw " + b("99 82800 82860 Europe/Kiev 1"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(1, "1-0 vw 99 82800 82860 Europe/Kiev 1")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        TimeInput timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[] {1}, timeInput.days);


        clientPair.hardwareClient.send("hardware vw " + b("99 82800 82860 Europe/Kiev "));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "1-0 vw 99 82800 82860 Europe/Kiev ")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 82800  Europe/Kiev "));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(3, "1-0 vw 99 82800  Europe/Kiev ")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = (clientPair.appClient.parseProfile(1));
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 82800  Europe/Kiev 1,2,3,4"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(4, "1-0 vw 99 82800  Europe/Kiev 1,2,3,4")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = (clientPair.appClient.parseProfile(1));
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99   Europe/Kiev 1,2,3,4"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(5, "1-0 vw 99   Europe/Kiev 1,2,3,4")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = (clientPair.appClient.parseProfile(1));
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-1, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 82800 82800 Europe/Kiev  10800"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(6, "1-0 vw 99 82800 82800 Europe/Kiev  10800")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82800, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 ss sr Europe/Kiev  10800"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(7, "1-0 vw 99 ss sr Europe/Kiev  10800")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-2, timeInput.startAt);
        assertEquals(-3, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);
    }

    @Test
    public void testWrongCommandForAggregation() throws Exception {
        clientPair.hardwareClient.send("hardware vw 10 aaaa");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "1-0 vw 10 aaaa")));
    }

    @Test
    public void testWrongPin() throws Exception {
        clientPair.hardwareClient.send("hardware vw x aaaa");
        clientPair.hardwareClient.verifyResult(illegalCommand(1));
    }

    @Test
    public void testAppSendWAwWorks() throws Exception {
        String body = "aw 8 333";
        clientPair.hardwareClient.send("hardware " + body);

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "1-0 aw 8 333")));
    }

    @Test
    public void testClosedConnectionWhenNotLogged() throws Exception {
        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.getDevice(1, 0);
        verify(appClient2.responseMock, after(600).never()).channelRead(any(), any());
        assertTrue(appClient2.isClosed());

        appClient2.login(getUserName(), "1", "Android", "1RC7");
        verify(appClient2.responseMock, after(200).never()).channelRead(any(), any());
    }

    @Test
    public void testRefreshTokenClosesExistingConnections() throws Exception {
        clientPair.appClient.send("refreshToken 1");
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
        assertTrue(clientPair.hardwareClient.isClosed());
    }

    @Test
    public void testSendPinModeCommandWhenHardwareGoesOnline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        assertTrue(channelFuture.isDone());

        String body = "vw 13 1";
        clientPair.appClient.send("hardware 1 " + body);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, DEVICE_NOT_IN_NETWORK)));

        TestHardClient hardClient = new TestHardClient("localhost", properties.getHttpPort());
        hardClient.start();
        hardClient.login(clientPair.token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, "pm 1 out 2 out 3 out 5 out 6 in 7 in 30 in 8 in")));
        verify(hardClient.responseMock, times(2)).channelRead(any(), any());
        hardClient.stop().awaitUninterruptibly();
    }

    @Test
    public void testSendGeneratedPinModeCommandWhenHardwareGoesOnline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.awaitUninterruptibly();

        assertTrue(channelFuture.isDone());

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, DEVICE_NOT_IN_NETWORK)));

        TestHardClient hardClient = new TestHardClient("localhost", properties.getHttpPort());
        hardClient.start();
        hardClient.login(clientPair.token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        String expectedBody = "pm 1 out 2 out 3 out 5 out 6 in 7 in 30 in 8 in";
        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(1, expectedBody)));
        verify(hardClient.responseMock, times(2)).channelRead(any(), any());
        hardClient.stop().awaitUninterruptibly();
    }

    @Test
    public void testSendHardwareCommandToNotActiveDashboard() throws Exception {
        clientPair.appClient.createDash("{\"id\":2,\"name\":\"My Dashboard2\"}");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.appClient.createDevice(2, new Device(2, "123", BoardType.ESP8266));
        Device device = clientPair.appClient.parseDevice();

        clientPair.appClient.reset();

        //connecting separate hardware to non active dashboard
        TestHardClient nonActiveDashHardClient = new TestHardClient("localhost", properties.getHttpPort());
        nonActiveDashHardClient.start();
        nonActiveDashHardClient.login(device.token);
        verify(nonActiveDashHardClient.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        nonActiveDashHardClient.reset();


        //sending hardware command from hardware that has no active dashboard
        nonActiveDashHardClient.send("hardware aw 1 1");
        //verify(nonActiveDashHardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NO_ACTIVE_DASHBOARD)));
        verify(clientPair.appClient.responseMock, timeout(1000).times(1)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(1000).times(1)).channelRead(any(), eq(hardwareConnected(1, "2-" + device.id)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.hardwareClient.responseMock, after(1000).never()).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(hardware(1, "1-0 aw 1 1")));
        nonActiveDashHardClient.stop().awaitUninterruptibly();
    }

    @Test
    public void testConnectAppAndHardwareAndSendCommands() throws Exception {
        for (int i = 0; i < 100; i++) {
            clientPair.appClient.send("hardware 1 aw 1 1");
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500).times(100)).channelRead(any(), any());
    }

    @Test
    public void testTryReachQuotaLimit() throws Exception {
        String body = "aw 100 100";

        //within 1 second sending more messages than default limit 100.
        for (int i = 0; i < 200; i++) {
            clientPair.hardwareClient.send("hardware " + body);
            TestUtil.sleep(5);
        }

        ArgumentCaptor<ResponseMessage> objectArgumentCaptor = ArgumentCaptor.forClass(ResponseMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<ResponseMessage> arguments = objectArgumentCaptor.getAllValues();
        ResponseMessage responseMessage = arguments.get(0);
        assertTrue(responseMessage.id > 100);

        //at least 100 iterations should be
        for (int i = 0; i < 100; i++) {
            verify(clientPair.appClient.responseMock).channelRead(any(), eq(hardware(i+1, "1-0 " + body)));
        }

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        //check no more accepted
        for (int i = 0; i < 10; i++) {
            clientPair.hardwareClient.send("hardware " + body);
            TestUtil.sleep(9);
        }

        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new ResponseMessage(1, QUOTA_LIMIT)));
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(hardware(1, body)));
    }

    @Test
    public void testCreateProjectWithDevicesGeneratesNewTokens() throws Exception {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 2;
        dashBoard.name = "Test Dash";

        Device device = new Device();
        device.id = 1;
        device.name = "MyDevice";
        device.token = "aaa";
        dashBoard.devices = new Device[] {
                device
        };

        clientPair.appClient.createDash(dashBoard);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("getDevices 2");

        Device[] devices = clientPair.appClient.parseDevices(2);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        assertEquals(1, devices[0].id);
        assertEquals("MyDevice", devices[0].name);
        assertNotEquals("aaa", devices[0].token);
    }

    @Test
    public void testButtonStateInPWMModeIsStored() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"DIGITAL\",\"pin\":18,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"value\":\"1\",\"pushMode\":false}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 aw 18 1032");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(2, "aw 18 1032")));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (short) 18, PinType.DIGITAL);
        assertNotNull(widget);
        assertEquals("1032", ((Button) widget).value);
    }

    @Test
    public void testTwoWidgetsOnTheSamePin() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"VIRTUAL\",\"pin\":37,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.createWidget(1, "{\"type\":\"BUTTON\",\"id\":1001,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"VIRTUAL\",\"pin\":37,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("hardware 1 vw 37 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(3, "vw 37 10")));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(3);

        int counter = 0;
        for (Widget widget : profile.dashBoards[0].widgets) {
            if (widget.isSame(0, (short) 37, PinType.VIRTUAL)) {
                counter++;
                assertEquals("10", ((OnePinWidget) widget).value);
            }
        }
        assertEquals(2, counter);

        clientPair.hardwareClient.send("hardware vw 37 11");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 37 11"));
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(5);
        counter = 0;
        for (Widget widget : profile.dashBoards[0].widgets) {
            if (widget.isSame(0, (short) 37, PinType.VIRTUAL)) {
                counter++;
                assertEquals("11", ((OnePinWidget) widget).value);
            }
        }
        assertEquals(2, counter);
    }

    @Test
    public void testButtonStateInPWMModeIsStoredWithUIHack() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"DIGITAL\",\"pin\":18,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"value\":\"1\",\"pushMode\":false}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 dw 18 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(hardware(2, "dw 18 1")));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (short) 18, PinType.DIGITAL);
        assertNotNull(widget);
        assertEquals("1", ((Button) widget).value);
    }

    @Test
    public void testOutdatedAppNotificationAlertWorks() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1", "Android", "1.1.1");
        appClient.verifyResult(ok(1));
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(
                appIsOutdated(1,
                        "Your app is outdated. Please update to the latest app version. " +
                                "Ignoring this notice may affect your projects.")));
    }

    @Test
    public void testOutdatedAppNotificationNotTriggered() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1", "Android", "1.1.2");
        appClient.verifyResult(ok(1));
        verify(appClient.responseMock, never()).channelRead(any(), eq(
                appIsOutdated(1,
                        "Your app is outdated. Please update to the latest app version. " +
                                "Ignoring this notice may affect your projects.")));
    }

    @Test
    public void newUserReceivesGrettingEmailAndNoIPLogged() throws Exception {
        TestAppClient appClient1 = new TestAppClient(properties);
        appClient1.start();

        appClient1.register("test@blynk.cc", "a", "Blynk");
        appClient1.verifyResult(ok(1));

        User user = holder.userDao.getByName("test@blynk.cc", "Blynk");
        assertNull(user.lastLoggedIP);

        verify(holder.mailWrapper).sendHtml(eq("test@blynk.cc"), eq("Get started with Blynk"), contains("Welcome to Blynk, a platform to build your next awesome IOT project."));

        appClient1.login("test@blynk.cc", "a");
        appClient1.verifyResult(ok(2));

        user = holder.userDao.getByName("test@blynk.cc", "Blynk");
        assertNull(user.lastLoggedIP);
    }

    @Test
    public void test() throws Exception {
            Twitter twitter = new Twitter();
            twitter.secret = "123";
            twitter.token = "124";

            DashBoard dash = new DashBoard();
            dash.sharedToken = "ffffffffffffffffffffffffffff";
            dash.widgets = new Widget[] {
                    twitter
            };

            System.out.println(JsonParser.init().writerFor(DashBoard.class).writeValueAsString(dash));
            System.out.println(JsonParser.init().writerFor(DashBoard.class).withView(View.PublicOnly.class).writeValueAsString(dash));
    }
}
