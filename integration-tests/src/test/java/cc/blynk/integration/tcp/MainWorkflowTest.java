package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DashboardSettings;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.Step;
import cc.blynk.server.core.model.widgets.others.Player;
import cc.blynk.server.core.model.widgets.ui.TimeInput;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetTokenMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareConnectedMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.ZoneId;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_INVALID_BODY;
import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_NOT_AUTHORIZED;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.server.core.protocol.enums.Response.NO_ACTIVE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Response.QUOTA_LIMIT;
import static cc.blynk.server.core.protocol.enums.Response.USER_ALREADY_REGISTERED;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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
public class MainWorkflowTest extends IntegrationBase {

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
    public void createBasicProfile() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("login test@test.com 1 Android RC13");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.send("createDash {\"id\":1, \"createdAt\":1, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        appClient.send("createWidget 1\0{\"id\":1, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":1}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(appClient.getBody());
        profile.dashBoards[0].updatedAt = 0;

        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());

        appClient.send("createWidget 1\0{\"id\":2, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = parseProfile(appClient.getBody());
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false},{\"type\":\"BUTTON\",\"id\":2,\"x\":2,\"y\":2,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text 2\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":2,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());

        appClient.send("updateWidget 1\0{\"id\":2, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"new label\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":3}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = parseProfile(appClient.getBody());
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false},{\"type\":\"BUTTON\",\"id\":2,\"x\":2,\"y\":2,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"new label\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":3,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());

        appClient.send("deleteWidget 1\0" + "3");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));

        appClient.send("deleteWidget 1\0" + "1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        appClient.send("deleteWidget 1\0" + "2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = parseProfile(appClient.getBody());
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());
    }

    @Test
    public void doNotAllowUsersWithQuestionMark() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register te?st@test.com 1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));
    }

    @Test
    public void createDashWithDevices() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("login test@test.com 1 Android RC13");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "AAAa";
        Device device = new Device();
        device.id = 0;
        device.name = "123";
        dash.devices = new Device[] {device};

        appClient.send("createDash no_token\0" + dash.toString());
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        appClient.send("getDevices 1");
        String response = appClient.getBody(4);

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        assertEquals(0, devices[0].id);
        assertEquals("123", devices[0].name);
        assertNull(devices[0].token);
    }

    @Test
    public void testConnectAppAndHardware() throws Exception {
        // we just test that app and hardware can actually connect
    }

    @Test
    public void testRegisterWithAnotherApp() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("login test@test.com 1 Android 1.13.3 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.send("createDash {\"id\":1, \"createdAt\":1, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        appClient.send("createWidget 1\0{\"id\":1, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":1}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(appClient.getBody());
        profile.dashBoards[0].updatedAt = 0;

        assertEquals("{\"dashBoards\":[{\"id\":1,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"Some Text\",\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());
    }

    @Test
    public void testDoubleLogin() throws Exception {
        clientPair.hardwareClient.send("login " + DEFAULT_TEST_USER + " 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, USER_ALREADY_REGISTERED)));
    }

    @Test
    public void testForwardBluetoothFromAppWorks() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":743, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"STEP\", \"pwmMode\":true, \"pinType\":\"VIRTUAL\", \"pin\":67}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardwareBT 1-0 vw 67 100");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 67 100"))));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody(2));
        assertNotNull(profile);
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 67, PinType.VIRTUAL);
        assertNotNull(widget);
        assertTrue(widget instanceof Step);
        assertEquals("100", ((OnePinWidget) widget).value);
    }

    @Test
    public void testValueForPWMPinForStteperIsAccepted() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":743, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"STEP\", \"pwmMode\":true, \"pinType\":\"DIGITAL\", \"pin\":24}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 aw 24 100");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("aw 24 100"))));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody(2));
        assertNotNull(profile);
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 24, PinType.DIGITAL);
        assertNotNull(widget);
        assertTrue(widget instanceof Step);
        assertEquals("100", ((OnePinWidget) widget).value);
    }

    @Test
    public void testNoEnergyDrainForBusinessApps() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.send("login test@test.com 1 Android 1.13.3 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.send("createDash {\"id\":2, \"createdAt\":1458856800001, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        appClient.send("getEnergy");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, GET_ENERGY, "2000")));

        appClient.send("createWidget 2\0{\"id\":2, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        appClient.send("createWidget 2\0{\"id\":3, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(6)));

        appClient.send("createWidget 2\0{\"id\":4, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(7)));

        appClient.send("createWidget 2\0{\"id\":5, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(8)));

        appClient.send("createWidget 2\0{\"id\":6, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(9)));

        appClient.send("createWidget 2\0{\"id\":7, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(10)));
    }

    @Test
    public void testPingCommandWorks() throws Exception {
        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
    }

    @Test
    public void testAddAndRemoveTabs() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "7500")));

        clientPair.appClient.send("createWidget 1\0{\"id\":100, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}, {\"label\":\"tab 3\"}], \"type\":\"TABS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("createWidget 1\0{\"id\":101, \"width\":1, \"height\":1, \"x\":15, \"y\":0, \"tabId\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":18}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, GET_ENERGY, "7100")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        assertEquals(19, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("deleteWidget 1\0" + "100");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, GET_ENERGY, "7300")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        assertEquals(17, profile.dashBoards[0].widgets.length);
        assertNotNull(profile.dashBoards[0].findWidgetByPin(0, (byte) 17, PinType.DIGITAL));
    }

    @Test
    public void testAddAndUpdateTabs() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "7500")));

        clientPair.appClient.send("createWidget 1\0{\"id\":100, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}, {\"label\":\"tab 3\"}], \"type\":\"TABS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("createWidget 1\0{\"id\":101, \"width\":1, \"height\":1, \"x\":15, \"y\":0, \"tabId\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":18}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":2, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, GET_ENERGY, "7100")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        assertEquals(19, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("updateWidget 1\0{\"id\":100, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}], \"type\":\"TABS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, GET_ENERGY, "7300")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        assertEquals(18, profile.dashBoards[0].widgets.length);
        assertNull(profile.dashBoards[0].findWidgetByPin(0, (byte) 17, PinType.DIGITAL));
        assertNotNull(profile.dashBoards[0].findWidgetByPin(0, (byte) 18, PinType.DIGITAL));
    }

    @Test
    public void testPurchaseEnergy() throws Exception {
        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "5262996016779471529.4493624392154338");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "A3B93EE9-BC65-499E-A660-F2A84F2AF1FC");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "com.blynk.energy.280001461578468247");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "150000195113772");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(6)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "1370-3990-1414-55681");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(7)));
    }

    @Test
    @Ignore
    public void testProfileMetadata() throws Exception {
        clientPair.appClient.send("saveMetadata {\"lat\":123.123,\"lon\":124.124}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getMetadata");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals("{\"lat\":123.123,\"lon\":124.124}", token);
    }

    @Test
    public void testApplicationPingCommandOk() throws Exception {
        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();

        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
    }

    @Test
    public void testHardPingCommandOk() throws Exception {
        clientPair.hardwareClient.send("ping");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("ping");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testDashCommands() throws Exception {
        clientPair.appClient.send("updateDash {\"id\":10, \"name\":\"test board update\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NOT_ALLOWED)));

        clientPair.appClient.send("updateDash {\"id\":10, \"name\":\"test board update\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.hardwareClient.send("ping");

        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(6, ILLEGAL_COMMAND)));

        Profile responseProfile;
        DashBoard responseDash;

        clientPair.appClient.send("loadProfileGzipped");
        responseProfile = parseProfile(clientPair.appClient.getBody(7));
        responseProfile.dashBoards[0].updatedAt = 0;
        responseProfile.dashBoards[0].createdAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false}]}", responseProfile.toString());

        clientPair.appClient.send("loadProfileGzipped 10");
        responseDash = JsonParser.parseDashboard(clientPair.appClient.getBody(8));
        responseDash.updatedAt = 0;
        responseDash.createdAt = 0;
        assertEquals("{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":false}", responseDash.toString());

        clientPair.appClient.send("loadProfileGzipped 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(9, ILLEGAL_COMMAND)));

        clientPair.appClient.send("activate 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(10, DEVICE_NOT_IN_NETWORK)));

        clientPair.appClient.send("loadProfileGzipped");
        responseProfile = parseProfile(clientPair.appClient.getBody(11));
        responseProfile.dashBoards[0].updatedAt = 0;
        responseProfile.dashBoards[0].createdAt = 0;
        String expectedProfile = "{\"dashBoards\":[{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":true}]}";
        assertEquals(expectedProfile, responseProfile.toString());

        clientPair.appClient.send("updateDash {\"id\":10,\"name\":\"test board update\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        expectedProfile = "{\"dashBoards\":[{\"id\":10,\"parentId\":-1,\"isPreview\":false,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":true}]}";
        clientPair.appClient.send("loadProfileGzipped");
        responseProfile = parseProfile(clientPair.appClient.getBody(13));
        responseProfile.dashBoards[0].updatedAt = 0;
        responseProfile.dashBoards[0].createdAt = 0;
        assertEquals(expectedProfile, responseProfile.toString());
    }

    @Test
    public void testHardwareChannelClosedOnDashRemoval() throws Exception {
        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        assertTrue(clientPair.hardwareClient.isClosed());
    }

    @Test
    public void testGetTokenWorksWithNewFormats() throws Exception {
        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 10");
        String token = clientPair.appClient.getBody(2);
        assertNotNull(token);

        clientPair.appClient.send("getToken 10-0");
        String token2 = clientPair.appClient.getBody(3);
        assertNotNull(token2);
        assertEquals(token, token2);

        clientPair.appClient.send("getToken " + b("10 0"));
        token2 = clientPair.appClient.getBody(4);
        assertNotNull(token2);
        assertEquals(token, token2);

        clientPair.appClient.send("createDash {\"id\":11, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("getToken " + b("11 0"));
        token2 = clientPair.appClient.getBody(6);
        assertNotNull(token2);
        assertNotEquals(token, token2);
    }

    @Test
    public void deleteDashDeletesTokensAlso() throws Exception {
        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getToken 10");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);

        clientPair.appClient.reset();
        clientPair.appClient.send("getShareToken 10");
        String sharedToken = clientPair.appClient.getBody();
        assertNotNull(sharedToken);

        clientPair.appClient.send("deleteDash 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        //todo on delete also close existing connections?
        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.send("login " + token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, INVALID_TOKEN)));

        TestAppClient newAppClient = new TestAppClient("localhost", tcpAppPort, properties);
        newAppClient.start();
        newAppClient.send("shareLogin " + "dima@mail.ua " + sharedToken + " Android 24");

        verify(newAppClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));
    }

    @Test
    public void loadGzippedProfile() throws Exception{
        Profile expectedProfile = JsonParser.parseProfileFromString(readTestUserProfile());

        clientPair.appClient.send("loadProfileGzipped");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), any());

        Profile profile = parseProfile(clientPair.appClient.getBody());

        profile.dashBoards[0].updatedAt = 0;

        expectedProfile.dashBoards[0].devices = null;
        profile.dashBoards[0].devices = null;

        assertEquals(expectedProfile.toString(), profile.toString());
    }

    @Test
    public void settingsUpdateCommand() throws Exception{
        DashboardSettings settings = new DashboardSettings("New Name", true, Theme.BlynkLight, true, true, false);

        clientPair.appClient.send("updateSettings 1\0" + JsonParser.toJson(settings));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody(2));
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
    public void testLargeMessageIsNotAccepted() throws Exception {
        for (int i = 0; i < 127 ; i++) {
            clientPair.hardwareClient.send("hardware vw " + i + " " + StringUtils.randomString(1000));
        }

        clientPair.appClient.send("loadProfileGzipped");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(serverError(1)));
    }

    @Test
    public void testSendUnicodeChar() throws Exception {
        clientPair.hardwareClient.send("hardware vw 1 °F");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 °F"))));
    }

    @Test
    public void testAppSendAnyHardCommandAndBack() throws Exception {
        clientPair.appClient.send("hardware 1 dw 1 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));

        clientPair.hardwareClient.send("hardware ar 2");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(2, HARDWARE, b("ar 2"))));
    }

    @Test
    public void testAppNoActiveDashForHard() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));
    }

    @Test
    public void testHardwareSendsWrongCommand() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 ");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(1)));

        clientPair.hardwareClient.send("hardware aw 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(2)));
    }

    @Test
    public void testAppChangeActiveDash() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        Profile newProfile = parseProfile(readTestUserProfile("user_profile_json_3_dashes.txt"));
        clientPair.appClient.send("createDash " + newProfile.dashBoards[1]);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(3, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, HARDWARE, b("1 aw 1 1"))));
    }

    @Test
    public void testActive2AndDeactivate1() throws Exception {
        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        Profile newProfile = parseProfile(readTestUserProfile("user_profile_json_3_dashes.txt"));
        clientPair.appClient.send("createDash " + newProfile.dashBoards[1]);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getToken 2");
        String token2 = clientPair.appClient.getBody();
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("2 aw 1 1"))));


        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("2 aw 1 1"))));
        hardClient2.stop().awaitUninterruptibly();
    }

    @Test
    public void testActivateWorkflow() throws Exception {
        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("deactivate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));

        clientPair.appClient.send("hardware 1 ar 1 1");
        //todo check no response
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));
    }

    @Test
    public void testTweetNotWorks() throws Exception {
        reset(blockingIOProcessor);

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

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, NOTIFICATION_NOT_AUTHORIZED)));
    }

    @Test
    public void testSmsWorks() throws Exception {
        reset(blockingIOProcessor);

        clientPair.hardwareClient.send("sms");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOTIFICATION_INVALID_BODY)));

        //no sms widget
        clientPair.hardwareClient.send("sms yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOTIFICATION_NOT_AUTHORIZED)));

        //adding sms widget
        clientPair.appClient.send("createWidget 1\0{\"id\":432, \"width\":1, \"height\":1, \"to\":\"3809683423423\", \"x\":0, \"y\":0, \"type\":\"SMS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("sms yo");
        verify(smsWrapper, timeout(500)).send(eq("3809683423423"), eq("yo"));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.hardwareClient.send("sms yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, QUOTA_LIMIT)));
    }

    @Test
    public void testTweetWorks() throws Exception {
        reset(blockingIOProcessor);

        clientPair.hardwareClient.send("tweet yo");
        verify(twitterWrapper, timeout(500)).send(eq("token"), eq("secret"), eq("yo"));

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, QUOTA_LIMIT)));
    }



    @Test
    public void testPlayerUpdateWorksAsExpected() throws Exception {
        clientPair.appClient.send(("createWidget 1\0{\"type\":\"PLAYER\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1}"));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw 99 play");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 play"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());
        Player player = (Player) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(player);
        assertTrue(player.isOnPlay);

        clientPair.appClient.send("hardware 1 vw 99 stop");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 stop"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        player = (Player) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(player);
        assertFalse(player.isOnPlay);
    }

    @Test
    public void testTimeInputUpdateWorksAsExpected() throws Exception {
        clientPair.appClient.send(("createWidget 1\0{\"type\":\"TIME_INPUT\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1}"));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82860 Europe/Kiev 1"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 82800 82860 Europe/Kiev 1"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());
        TimeInput timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[] {1}, timeInput.days);


        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82860 Europe/Kiev "));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 82800 82860 Europe/Kiev "))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 82800  Europe/Kiev "));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 82800  Europe/Kiev "))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 82800  Europe/Kiev 1,2,3,4"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 82800  Europe/Kiev 1,2,3,4"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99   Europe/Kiev 1,2,3,4"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99   Europe/Kiev 1,2,3,4"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-1, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82800 Europe/Kiev  10800"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 82800 82800 Europe/Kiev  10800"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82800, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.appClient.send("hardware 1 vw " + b("99 ss sr Europe/Kiev  10800"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 ss sr Europe/Kiev  10800"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-2, timeInput.startAt);
        assertEquals(-3, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);
    }

    @Test
    public void testTimeInputUpdateWorksAsExpectedFromHardSide() throws Exception {
        clientPair.appClient.send(("createWidget 1\0{\"type\":\"TIME_INPUT\",\"orgId\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1}"));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.hardwareClient.send("hardware vw " + b("99 82800 82860 Europe/Kiev 1"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 99 82800 82860 Europe/Kiev 1"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());
        TimeInput timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[] {1}, timeInput.days);


        clientPair.hardwareClient.send("hardware vw " + b("99 82800 82860 Europe/Kiev "));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("1 vw 99 82800 82860 Europe/Kiev "))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 82800  Europe/Kiev "));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(3, HARDWARE, b("1 vw 99 82800  Europe/Kiev "))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 82800  Europe/Kiev 1,2,3,4"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(4, HARDWARE, b("1 vw 99 82800  Europe/Kiev 1,2,3,4"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99   Europe/Kiev 1,2,3,4"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(5, HARDWARE, b("1 vw 99   Europe/Kiev 1,2,3,4"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-1, timeInput.startAt);
        assertEquals(-1, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[]{1,2,3,4}, timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 82800 82800 Europe/Kiev  10800"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(6, HARDWARE, b("1 vw 99 82800 82800 Europe/Kiev  10800"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82800, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);

        clientPair.hardwareClient.send("hardware vw " + b("99 ss sr Europe/Kiev  10800"));
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(7, HARDWARE, b("1 vw 99 ss sr Europe/Kiev  10800"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());
        timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (byte) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(-2, timeInput.startAt);
        assertEquals(-3, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertNull(timeInput.days);
    }

    @Test
    public void testWrongCommandForAggregation() throws Exception {
        clientPair.hardwareClient.send("hardware vw 10 aaaa");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 10 aaaa"))));
    }

    @Test
    public void testWrongPin() throws Exception {
        clientPair.hardwareClient.send("hardware vw x aaaa");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));
    }

    @Test
    public void testAppSendWAwWorks() throws Exception {
        String body = "aw 8 333";
        clientPair.hardwareClient.send("hardware " + body);

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 8 333"))));
    }

    @Test
    public void testClosedConnectionWhenNotLogged() throws Exception {
        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("getToken 1");
        verify(appClient2.responseMock, after(600).never()).channelRead(any(), any());
        assertTrue(appClient2.isClosed());

        appClient2.send("login dima@mail.ua 1 Android 1RC7");
        verify(appClient2.responseMock, after(200).never()).channelRead(any(), any());
    }

    @Test
    public void testRefreshTokenClosesExistingConnections() throws Exception {
        clientPair.appClient.send("refreshToken 1");
        String newToken = clientPair.appClient.getBody();
        assertNotNull(newToken);
        assertEquals(32, newToken.length());
        assertTrue(clientPair.hardwareClient.isClosed());

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();
        hardClient.send("login " + newToken);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
    }

    @Test
    public void testSendPinModeCommandWhenHardwareGoesOnline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        assertTrue(channelFuture.isDone());

        String body = "vw 13 1";
        clientPair.appClient.send("hardware 1 " + body);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, DEVICE_NOT_IN_NETWORK)));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();
        hardClient.send("login " + clientPair.token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("pm 1 out 2 out 3 out 5 out 6 in 7 in 30 in 8 in"))));
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

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();
        hardClient.send("login " + clientPair.token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        String expectedBody = "pm 1 out 2 out 3 out 5 out 6 in 7 in 30 in 8 in";
        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b(expectedBody))));
        verify(hardClient.responseMock, times(2)).channelRead(any(), any());
        hardClient.stop().awaitUninterruptibly();
    }

    @Test
    public void testSendHardwareCommandToNotActiveDashboard() throws Exception {
        clientPair.appClient.send("createDash " + "{\"id\":2,\"name\":\"My Dashboard2\"}");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getToken 2");

        //getting token for second GetTokenMessage
        ArgumentCaptor<GetTokenMessage> objectArgumentCaptor = ArgumentCaptor.forClass(GetTokenMessage.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());
        List<GetTokenMessage> arguments = objectArgumentCaptor.getAllValues();
        GetTokenMessage getTokenMessage = arguments.get(0);
        String token = getTokenMessage.body;

        clientPair.appClient.reset();

        //connecting separate hardware to non active dashboard
        TestHardClient nonActiveDashHardClient = new TestHardClient("localhost", tcpHardPort);
        nonActiveDashHardClient.start();
        nonActiveDashHardClient.send("login " + token);
        verify(nonActiveDashHardClient.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        nonActiveDashHardClient.reset();


        //sending hardware command from hardware that has no active dashboard
        nonActiveDashHardClient.send("hardware aw 1 1");
        //verify(nonActiveDashHardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NO_ACTIVE_DASHBOARD)));
        verify(clientPair.appClient.responseMock, timeout(1000).times(1)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(1000).times(1)).channelRead(any(), eq(new HardwareConnectedMessage(1, "2-0")));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.hardwareClient.responseMock, after(1000).never()).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));
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
            sleep(5);
        }

        ArgumentCaptor<ResponseMessage> objectArgumentCaptor = ArgumentCaptor.forClass(ResponseMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<ResponseMessage> arguments = objectArgumentCaptor.getAllValues();
        ResponseMessage responseMessage = arguments.get(0);
        assertTrue(responseMessage.id > 100);

        //at least 100 iterations should be
        for (int i = 0; i < 100; i++) {
            verify(clientPair.appClient.responseMock).channelRead(any(), eq(produce(i+1, HARDWARE, b("1 " + body))));
        }

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        //check no more accepted
        for (int i = 0; i < 10; i++) {
            clientPair.hardwareClient.send("hardware " + body);
            sleep(9);
        }

        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new ResponseMessage(1, QUOTA_LIMIT)));
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(produce(1, HARDWARE, b(body))));
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

        clientPair.appClient.send("createDash " + dashBoard.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getDevices 2");
        String response = clientPair.appClient.getBody(2);

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        assertEquals(1, devices[0].id);
        assertEquals("MyDevice", devices[0].name);
        assertNotEquals("aaa", devices[0].token);
    }

    @Test
    @Ignore("hard to test this case...")
    public void testTryReachQuotaLimitAndWarningExceededLimit() throws Exception {
        String body = "1 ar 100 100";

        //within 1 second sending more messages than default limit 100.
        for (int i = 0; i < 1000 / 9; i++) {
            clientPair.appClient.send("hardware " + body, 1);
            sleep(9);
        }

        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, QUOTA_LIMIT)));
        verify(clientPair.hardwareClient.responseMock, atLeast(100)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        //waiting to avoid limit.
        sleep(1000);

        for (int i = 0; i < 100000 / 9; i++) {
            clientPair.appClient.send("hardware " + body, 1);
            sleep(9);
        }

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, QUOTA_LIMIT)));
        verify(clientPair.hardwareClient.responseMock, atLeast(100)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));
    }

    @Test
    public void testButtonStateInPWMModeIsStored() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"DIGITAL\",\"pin\":18,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"value\":\"1\",\"pushMode\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 aw 18 1032");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("aw 18 1032"))));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody(2));
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 18, PinType.DIGITAL);
        assertNotNull(widget);
        assertEquals("1032", ((Button) widget).value);
    }

    @Test
    public void testButtonStateInPWMModeIsStoredWithUIHack() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"DIGITAL\",\"pin\":18,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"value\":\"1\",\"pushMode\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 dw 18 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("dw 18 1"))));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody(2));
        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 18, PinType.DIGITAL);
        assertNotNull(widget);
        assertEquals("1", ((Button) widget).value);
    }
}
