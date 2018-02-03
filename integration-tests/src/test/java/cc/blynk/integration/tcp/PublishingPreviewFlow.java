package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.AppAndHttpsServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.outputs.TextAlignment;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileMode;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.server.notifications.mail.QrHolder;
import cc.blynk.utils.AppNameUtil;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static cc.blynk.server.core.model.serialization.JsonParser.MAPPER;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
public class PublishingPreviewFlow extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        holder = new Holder(properties, twitterWrapper, mailWrapper, gcmWrapper, smsWrapper, "db-test.properties");

        assertNotNull(holder.dbManager.getConnection());

        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppAndHttpsServer(holder).start();

        this.clientPair = initAppAndHardPair();
        holder.dbManager.executeSQL("DELETE FROM flashed_tokens");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testGetProjectByToken() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(), 0);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.staticMailBody.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

        clientPair.appClient.send("getProjectByToken " + qrHolders[0].token);
        String body = clientPair.appClient.getBody(3);
        assertNotNull(body);
        DashBoard dashBoard = JsonParser.parseDashboard(body, 0);
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
    }

    @Test
    public void testSendStaticEmailForAppPublish() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(), 0);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.staticMailBody.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

        FlashedToken flashedToken = holder.dbManager.selectFlashedToken(qrHolders[0].token);
        assertNotNull(flashedToken);
        assertEquals(flashedToken.appId, app.id);
        assertEquals(1, flashedToken.dashId);
        assertEquals(0, flashedToken.deviceId);
        assertEquals(qrHolders[0].token, flashedToken.token);
        assertEquals(false, flashedToken.isActivated);
    }

    @Test
    public void testSendDynamicEmailForAppPublish() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(), 0);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);

        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.dynamicMailBody.replace("{project_name}", "My Dashboard")), eq(qrHolders));
    }

    @Test
    public void testSendDynamicEmailForAppPublishWithFewDevices() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        device1 = JsonParser.parseDevice(clientPair.appClient.getBody(1), 0);
        assertNotNull(device1);
        assertEquals(1, device1.id);

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(2), 0);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertEquals(2, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, true);

        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.dynamicMailBody.replace("{project_name}", "My Dashboard")), eq(qrHolders));
    }

    @Test
    public void testFaceEditNotAllowedHasNoChild() throws Exception {
        clientPair.appClient.send("updateFace 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(1)));
    }

    @Test
    public void testFaceUpdateWorks() throws Exception {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";

        clientPair.appClient.createDash(dashBoard);

        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;

        clientPair.appClient.createDevice(10, device0);
        String createdDevice = clientPair.appClient.getBody(2);
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(2, device.toString())));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(3), 0);
        assertNotNull(app);
        assertNotNull(app.id);


        clientPair.appClient.send("emailQr 10\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(4)));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.staticMailBody.replace("{project_name}", "Face Edit Test").replace("{device_section}", sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        appClient2.send("loadProfileGzipped");
        Profile profile = parseProfile(appClient2.getBody(3));
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(0, dashBoard.widgets.length);

        clientPair.appClient.send("updateFace 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        appClient2.send("loadProfileGzipped");
        profile = parseProfile(appClient2.getBody(4));
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(16, dashBoard.widgets.length);
    }

    @Test
    public void testUpdateFaceDoesntEraseExistingDeviceTiles() throws Exception {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";

        clientPair.appClient.createDash(dashBoard);

        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;

        clientPair.appClient.createDevice(10, device0);
        String createdDevice = clientPair.appClient.getBody(2);
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(2, device.toString())));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(3), 0);
        assertNotNull(app);
        assertNotNull(app.id);


        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        //creating manually widget for child project
        clientPair.appClient.createWidget(10, deviceTiles);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null);

        clientPair.appClient.send("createTemplate " + b("10 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        //creating manually widget for parent project
        clientPair.appClient.send("addEnergy " + "10000" + "\0" + "1370-3990-1414-55681");

        clientPair.appClient.createWidget(1, deviceTiles);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(7)));

        tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(8)));


        clientPair.appClient.send("emailQr 10\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(9)));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.staticMailBody.replace("{project_name}", "Face Edit Test").replace("{device_section}", sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        appClient2.send("loadProfileGzipped");
        Profile profile = parseProfile(appClient2.getBody(3));
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(1, dashBoard.widgets.length);
        assertTrue(dashBoard.widgets[0] instanceof DeviceTiles);

        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null);
        appClient2.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));


        clientPair.appClient.send("updateFace 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(10)));

        appClient2.send("loadProfileGzipped");
        profile = parseProfile(appClient2.getBody(5));
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(17, dashBoard.widgets.length);
        deviceTiles = (DeviceTiles) dashBoard.getWidgetById(widgetId);
        assertNotNull(deviceTiles);
        assertNotNull(deviceTiles.tiles);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(1, deviceTiles.tiles[0].templateId);
        assertEquals(1, deviceTiles.templates.length);
    }

    @Test
    public void testFaceEditForRestrictiveFields() throws Exception {
        Profile profile = JsonParser.parseProfileFromString(readTestUserProfile());

        DashBoard dashBoard = profile.dashBoards[0];
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";
        dashBoard.devices = null;

        clientPair.appClient.createDash(dashBoard);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        Device device0 = new Device(0, "My Device", "UNO");
        device0.status = Status.ONLINE;

        clientPair.appClient.createDevice(10, device0);
        String createdDevice = clientPair.appClient.getBody(2);
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(2, device.toString())));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(3), 0);
        assertNotNull(app);
        assertNotNull(app.id);


        clientPair.appClient.send("emailQr 10\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(4)));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.staticMailBody.replace("{project_name}", "Face Edit Test").replace("{device_section}", sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        appClient2.send("loadProfileGzipped");
        profile = parseProfile(appClient2.getBody(3));
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(16, dashBoard.widgets.length);

        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.updateWidget(1, "{\"id\":10, \"height\":2, \"width\":1, \"x\":22, \"y\":23, \"username\":\"pupkin@gmail.com\", \"token\":\"token\", \"secret\":\"secret\", \"type\":\"TWITTER\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(6)));

        clientPair.appClient.send("updateFace 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(7)));

        appClient2.send("loadProfileGzipped");
        profile = parseProfile(appClient2.getBody(4));
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(16, dashBoard.widgets.length);
        Notification notification = dashBoard.getWidgetByType(Notification.class);
        assertEquals(0, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());
        Twitter twitter = dashBoard.getWidgetByType(Twitter.class);
        assertNull(twitter.username);
        assertNull(twitter.token);
        assertNull(twitter.secret);
        assertEquals(22, twitter.x);
        assertEquals(23, twitter.y);
    }

    @Test
    public void testDeleteWorksForPreviewApp() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(), 0);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);

        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.staticMailBody.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].token + " " + qrHolders[0].dashId + " " + DEFAULT_TEST_USER);

        DashBoard dashBoard = JsonParser.parseDashboard(clientPair.appClient.getBody(3), 0);
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.devices);
        assertNull(dashBoard.devices[0].token);
        assertNull(dashBoard.devices[0].lastLoggedIP);
        assertEquals(0, dashBoard.devices[0].disconnectTime);
        assertEquals(Status.OFFLINE, dashBoard.devices[0].status);

        dashBoard.id = 2;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;

        clientPair.appClient.createDash(dashBoard);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.appClient.send("deleteDash 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("loadProfileGzipped 1");
        dashBoard = JsonParser.parseDashboard(clientPair.appClient.getBody(6), 0);
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);

        clientPair.appClient.send("loadProfileGzipped 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(7, ILLEGAL_COMMAND)));
    }

    @Test
    public void testDeleteWorksForParentOfPreviewApp() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(), 0);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);

        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.staticMailBody.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].token + " " + qrHolders[0].dashId + " " + DEFAULT_TEST_USER);

        DashBoard dashBoard = JsonParser.parseDashboard(clientPair.appClient.getBody(3), 0);
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.devices);
        assertNull(dashBoard.devices[0].token);
        assertNull(dashBoard.devices[0].lastLoggedIP);
        assertEquals(0, dashBoard.devices[0].disconnectTime);
        assertEquals(Status.OFFLINE, dashBoard.devices[0].status);

        dashBoard.id = 2;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;

        clientPair.appClient.createDash(dashBoard);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfileFromString(clientPair.appClient.getBody(6));
        assertNotNull(profile);
        assertNotNull(profile.dashBoards);
        assertEquals(1, profile.dashBoards.length);

        clientPair.appClient.send("loadProfileGzipped 2");
        response = clientPair.appClient.getBody(7);
        assertNotNull(response);
    }

    @Test
    public void testExportedAppFlowWithOneDynamicTest() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(), 0);
        assertNotNull(app);
        assertNotNull(app.id);

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        appClient2.send("loadProfileGzipped 1");
        DashBoard dashBoard = JsonParser.parseDashboard(appClient2.getBody(3), 0);
        assertNotNull(dashBoard);

        Device device = dashBoard.devices[0];
        assertNotNull(device);
        assertNotNull(device.token);

        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();

        hardClient1.login(device.token);
        verify(hardClient1.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        verify(appClient2.responseMock, timeout(2000)).channelRead(any(), eq(hardwareConnected(1, "1-0")));

        hardClient1.send("hardware vw 1 100");
        verify(appClient2.responseMock, timeout(2000)).channelRead(any(), eq(new HardwareMessage(2, b("1-0 vw 1 100"))));
    }

    @Test
    public void testFullDynamicAppFlow() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(), 0);
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.hardwareClient.send("hardware dw 1 abc");
        clientPair.hardwareClient.send("hardware vw 77 123");

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(),
                eq(new HardwareMessage(1, b("1-0 dw 1 abc"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(),
                eq(new HardwareMessage(2, b("1-0 vw 77 123"))));

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = JsonParser.parseDashboard(clientPair.appClient.getBody(4), 0);
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.pinsStorage);
        assertEquals(1, dashBoard.pinsStorage.size());
        Widget w = dashBoard.findWidgetByPin(0, (byte) 1, PinType.DIGITAL);
        assertNotNull(w);
        assertEquals("abc", ((OnePinWidget) w).value);

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        appClient2.send("loadProfileGzipped " + qrHolders[0].token + "\0" + 1 + "\0" + DEFAULT_TEST_USER + "\0" + AppNameUtil.BLYNK);
        dashBoard = JsonParser.parseDashboard(appClient2.getBody(3), 0);
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.pinsStorage);
        assertTrue(dashBoard.pinsStorage.isEmpty());
        w = dashBoard.findWidgetByPin(0, (byte) 1, PinType.DIGITAL);
        assertNotNull(w);
        assertNull(((OnePinWidget) w).value);

        Device device = dashBoard.devices[0];
        assertNotNull(device);
        assertNull(device.token);

        appClient2.reset();
        appClient2.send("getToken 1-" + device.id);
        String token = appClient2.getBody();

        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();

        hardClient1.login(token);
        verify(hardClient1.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        verify(appClient2.responseMock, timeout(2000)).channelRead(any(), eq(hardwareConnected(1, "1-0")));

        hardClient1.send("hardware vw 1 100");
        verify(appClient2.responseMock, timeout(2000)).channelRead(any(), eq(new HardwareMessage(2, b("1-0 vw 1 100"))));
    }

    private QrHolder[] makeQRs(Device[] devices, int dashId, boolean onlyFirst) throws Exception {
        QrHolder[] qrHolders;
        if (onlyFirst) {
            qrHolders = new QrHolder[1];
        } else {
            qrHolders = new QrHolder[devices.length];
        }

        List<FlashedToken> flashedTokens = getAllTokens();

        int i = 0;
        for (Device device : devices) {
            if (onlyFirst && i > 0) {
                break;
            }
            String newToken = flashedTokens.get(i).token;
            qrHolders[i] = new QrHolder(dashId, device.id, device.name, newToken, QRCode.from(newToken).to(ImageType.JPG).stream().toByteArray());
            i++;
        }

        return qrHolders;
    }

    private List<FlashedToken> getAllTokens() throws Exception {
        List<FlashedToken> list = new ArrayList<>();
        try (Connection connection = holder.dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from flashed_tokens")) {

            int i = 0;
            if (rs.next()) {
                list.add(new FlashedToken(rs.getString("token"), rs.getString("app_name"),
                        rs.getString("email"), rs.getInt("project_id"), rs.getInt("device_id"),
                        rs.getBoolean("is_activated"), rs.getDate("ts")
                ));
            }
            connection.commit();
        }
        return list;
    }

}
