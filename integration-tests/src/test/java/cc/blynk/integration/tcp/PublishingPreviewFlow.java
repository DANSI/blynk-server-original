package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTestWithDB;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.outputs.Gauge;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.server.core.model.widgets.ui.TimeInput;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.model.widgets.ui.tiles.templates.PageTileTemplate;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.server.notifications.mail.QrHolder;
import cc.blynk.utils.AppNameUtil;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static cc.blynk.integration.TestUtil.appSync;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.deviceOffline;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.hardwareConnected;
import static cc.blynk.integration.TestUtil.illegalCommand;
import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.readTestUserProfile;
import static cc.blynk.integration.TestUtil.sleep;
import static cc.blynk.server.core.model.serialization.JsonParser.MAPPER;
import static cc.blynk.utils.properties.Placeholders.DYNAMIC_SECTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class PublishingPreviewFlow extends SingleServerInstancePerTestWithDB {

    @Before
    public void deleteTable() throws Exception {
        holder.dbManager.executeSQL("DELETE FROM flashed_tokens");
    }

    @Test
    public void testGetProjectByToken() throws Exception {
        App appObj = new App(null, Theme.BlynkLight,
                ProvisionType.STATIC,
                0, false, "AppPreview", "myIcon", new int[] {1});
        clientPair.appClient.createApp(appObj);
        App appFromApi = clientPair.appClient.parseApp(1);
        assertNotNull(appFromApi);
        assertNotNull(appFromApi.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + appFromApi.id);
        clientPair.appClient.verifyResult(ok(2));

        QrHolder[] qrHolders = makeQRs(devices, 1);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.staticMailBody.replace("{project_name}", "My Dashboard").replace(DYNAMIC_SECTION, sb.toString())), eq(qrHolders));

        clientPair.appClient.send("getProjectByToken " + qrHolders[0].token);
        DashBoard dashBoard = clientPair.appClient.parseDash(3);
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
    }

    @Test
    public void testSendStaticEmailForAppPublish() throws Exception {
        App appObj = new App(null, Theme.BlynkLight,
                ProvisionType.STATIC,
                0, false, "AppPreview", "myIcon", new int[] {1});
        clientPair.appClient.createApp(appObj);
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        clientPair.appClient.verifyResult(ok(2));

        QrHolder[] qrHolders = makeQRs(devices, 1);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.staticMailBody.replace("{project_name}", "My Dashboard").replace(DYNAMIC_SECTION, sb.toString())), eq(qrHolders));

        FlashedToken flashedToken = holder.dbManager.selectFlashedToken(qrHolders[0].token);
        assertNotNull(flashedToken);
        assertEquals(flashedToken.appId, app.id);
        assertEquals(1, flashedToken.dashId);
        assertEquals(0, flashedToken.deviceId);
        assertEquals(qrHolders[0].token, flashedToken.token);
        assertFalse(flashedToken.isActivated);
    }

    @Test
    public void testSendDynamicEmailForAppPublish() throws Exception {
        App appObj = new App(null, Theme.BlynkLight,
                ProvisionType.DYNAMIC,
                0, false, "AppPreview", "myIcon", new int[] {1});
        clientPair.appClient.createApp(appObj);
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        clientPair.appClient.verifyResult(ok(2));

        FlashedToken flashedToken = getFlashedTokenByDevice();
        assertNotNull(flashedToken);
        QrHolder qrHolder = new QrHolder(1, -1, null, flashedToken.token, QRCode.from(flashedToken.token).to(ImageType.JPG).stream().toByteArray());

        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.dynamicMailBody.replace("{project_name}", "My Dashboard")), eq(qrHolder));
    }

    @Test
    public void testSendDynamicEmailForAppPublishAndNoDevices() throws Exception {
        App appObj = new App(null, Theme.BlynkLight,
                ProvisionType.DYNAMIC,
                0, false, "AppPreview", "myIcon", new int[] {1});
        clientPair.appClient.createApp(appObj);
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.deleteDevice(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices(1);
        assertEquals(0, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        clientPair.appClient.verifyResult(ok(2));

        FlashedToken flashedToken = getFlashedTokenByDevice();
        assertNotNull(flashedToken);
        QrHolder qrHolder = new QrHolder(1, -1, null, flashedToken.token, QRCode.from(flashedToken.token).to(ImageType.JPG).stream().toByteArray());

        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.dynamicMailBody.replace("{project_name}", "My Dashboard")), eq(qrHolder));
    }

    @Test
    public void testSendDynamicEmailForAppPublishWithFewDevices() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        device1 = clientPair.appClient.parseDevice();
        assertNotNull(device1);
        assertEquals(1, device1.id);

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(2);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertEquals(2, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        clientPair.appClient.verifyResult(ok(2));

        FlashedToken flashedToken = getFlashedTokenByDevice();
        assertNotNull(flashedToken);
        QrHolder qrHolder = new QrHolder(1, -1, null, flashedToken.token, QRCode.from(flashedToken.token).to(ImageType.JPG).stream().toByteArray());

        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.dynamicMailBody.replace("{project_name}", "My Dashboard")), eq(qrHolder));
    }

    @Test
    public void testFaceEditNotAllowedHasNoChild() throws Exception {
        clientPair.appClient.send("updateFace 1");
        clientPair.appClient.verifyResult(notAllowed(1));
    }

    @Test
    public void testFaceUpdateWorks() throws Exception {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";

        clientPair.appClient.createDash(dashBoard);

        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;

        clientPair.appClient.createDevice(10, device0);
        Device device = clientPair.appClient.parseDevice(2);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(2, device)));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = clientPair.appClient.parseApp(3);
        assertNotNull(app);
        assertNotNull(app.id);


        clientPair.appClient.send("emailQr 10\0" + app.id);
        clientPair.appClient.verifyResult(ok(4));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.staticMailBody.replace("{project_name}", "Face Edit Test").replace(DYNAMIC_SECTION, sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        appClient2.send("loadProfileGzipped");
        Profile profile = appClient2.parseProfile(3);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(0, dashBoard.widgets.length);

        clientPair.appClient.send("updateFace 1");
        clientPair.appClient.verifyResult(ok(5));
        assertTrue(appClient2.isClosed());

        appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(2);
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

        Device device0 = new Device(0, "My Dashboard", BoardType.ESP8266);
        device0.status = Status.ONLINE;

        clientPair.appClient.createDevice(10, device0);
        Device device = clientPair.appClient.parseDevice(2);
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(2, device));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = clientPair.appClient.parseApp(3);
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
        clientPair.appClient.verifyResult(ok(4));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "123", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.send("createTemplate " + b("10 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(5));

        //creating manually widget for parent project
        clientPair.appClient.send("addEnergy " + "10000" + "\0" + "1370-3990-1414-55681");

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(7));

        tileTemplate = new PageTileTemplate(1,
                null, null, "123", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(8));


        clientPair.appClient.send("emailQr 10\0" + app.id);
        clientPair.appClient.verifyResult(ok(9));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.staticMailBody.replace("{project_name}", "Face Edit Test").replace(DYNAMIC_SECTION, sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(2));

        appClient2.send("loadProfileGzipped");
        Profile profile = appClient2.parseProfile(3);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(1, dashBoard.widgets.length);
        assertTrue(dashBoard.widgets[0] instanceof DeviceTiles);
        deviceTiles = (DeviceTiles) dashBoard.getWidgetById(widgetId);
        assertNotNull(deviceTiles.tiles);
        assertNotNull(deviceTiles.templates);
        assertEquals(0, deviceTiles.tiles.length);
        assertEquals(1, deviceTiles.templates.length);

        tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "123", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);
        appClient2.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        appClient2.verifyResult(ok(4));


        clientPair.appClient.send("updateFace 1");
        clientPair.appClient.verifyResult(ok(10));
        assertTrue(appClient2.isClosed());

        appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(2);
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
        assertNotNull(deviceTiles.templates[0].deviceIds);
        assertEquals(1, deviceTiles.templates[0].deviceIds.length);
    }

    @Test
    public void testDeviceTilesAreNotCopiedFromParentProjectOnCreationAndFaceUpdate() throws Exception {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";

        clientPair.appClient.createDash(dashBoard);

        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        clientPair.appClient.createDevice(10, device0);
        device0 = clientPair.appClient.parseDevice(2);
        clientPair.appClient.verifyResult(createDevice(2, device0));

        Device device2 = new Device(2, "My Dashboard", BoardType.Arduino_UNO);
        clientPair.appClient.createDevice(10, device2);
        device2 = clientPair.appClient.parseDevice(3);
        clientPair.appClient.verifyResult(createDevice(3, device2));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = clientPair.appClient.parseApp(4);
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
        clientPair.appClient.verifyResult(ok(5));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {2}, "123", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.send("createTemplate " + b("10 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.createWidget(10, "{\"id\":155, \"deviceId\":0, \"frequency\":400, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":100}");
        clientPair.appClient.verifyResult(ok(7));

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(2));

        appClient2.send("loadProfileGzipped");
        Profile profile = appClient2.parseProfile(3);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(1, dashBoard.devices.length);
        assertEquals(0, dashBoard.devices[0].id);
        assertEquals(2, dashBoard.widgets.length);
        assertTrue(dashBoard.widgets[0] instanceof DeviceTiles);
        deviceTiles = (DeviceTiles) dashBoard.getWidgetById(widgetId);
        assertNotNull(deviceTiles.tiles);
        assertNotNull(deviceTiles.templates);
        assertEquals(0, deviceTiles.tiles.length);
        assertEquals(1, deviceTiles.templates.length);
    }

    @Test
    public void testChildProjectDoesntGetParentValues() throws Exception {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";

        clientPair.appClient.createDash(dashBoard);

        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        clientPair.appClient.createDevice(10, device0);
        device0 = clientPair.appClient.parseDevice(2);
        clientPair.appClient.verifyResult(createDevice(2, device0));

        Device device2 = new Device(2, "My Dashboard", BoardType.Arduino_UNO);
        clientPair.appClient.createDevice(10, device2);
        device2 = clientPair.appClient.parseDevice(3);
        clientPair.appClient.verifyResult(createDevice(3, device2));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = clientPair.appClient.parseApp(4);
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
        clientPair.appClient.verifyResult(ok(5));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {2}, "123", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.send("createTemplate " + b("10 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.createWidget(1, "{\"id\":155, \"value\":\"data\", \"deviceId\":0, \"frequency\":400, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":100}");
        clientPair.appClient.verifyResult(ok(7));

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(2));

        appClient2.send("loadProfileGzipped");
        Profile profile = appClient2.parseProfile(3);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(1, dashBoard.widgets.length);
        assertTrue(dashBoard.widgets[0] instanceof DeviceTiles);
        deviceTiles = (DeviceTiles) dashBoard.getWidgetById(widgetId);
        assertNotNull(deviceTiles.tiles);
        assertNotNull(deviceTiles.templates);
        assertEquals(0, deviceTiles.tiles.length);
        assertEquals(1, deviceTiles.templates.length);
        Gauge gauge = (Gauge) dashBoard.getWidgetById(155);
        assertNull(gauge);

        clientPair.appClient.send("updateFace 1");
        clientPair.appClient.verifyResult(ok(8));

        if (!appClient2.isClosed()) {
            sleep(300);
        }
        assertTrue(appClient2.isClosed());

        appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(2);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        gauge = (Gauge) dashBoard.getWidgetById(155);
        assertNotNull(gauge);
        assertNull(gauge.value);

        //one more time, to check another branch
        clientPair.appClient.send("updateFace 1");
        clientPair.appClient.verifyResult(ok(9));

        assertTrue(appClient2.isClosed());

        appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(2);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        gauge = (Gauge) dashBoard.getWidgetById(155);
        assertNotNull(gauge);
        assertNull(gauge.value);
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
        clientPair.appClient.verifyResult(ok(1));

        Device device0 = new Device(0, "My Device", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;

        clientPair.appClient.createDevice(10, device0);
        Device device = clientPair.appClient.parseDevice(2);
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(2, device));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = clientPair.appClient.parseApp(3);
        assertNotNull(app);
        assertNotNull(app.id);


        clientPair.appClient.send("emailQr 10\0" + app.id);
        clientPair.appClient.verifyResult(ok(4));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.staticMailBody.replace("{project_name}", "Face Edit Test").replace(DYNAMIC_SECTION, sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(2));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(3);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(16, dashBoard.widgets.length);

        clientPair.appClient.send("addPushToken 1\0uid1\0token1");
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.updateWidget(1, "{\"id\":10, \"height\":2, \"width\":1, \"x\":22, \"y\":23, \"username\":\"pupkin@gmail.com\", \"token\":\"token\", \"secret\":\"secret\", \"type\":\"TWITTER\"}");
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.send("updateFace 1");
        clientPair.appClient.verifyResult(ok(7));

        assertTrue(appClient2.isClosed());

        appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(2);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(16, dashBoard.widgets.length);
        Notification notification = dashBoard.getNotificationWidget();
        assertEquals(0, notification.androidTokens.size());
        assertEquals(0, notification.iOSTokens.size());
        Twitter twitter = dashBoard.getTwitterWidget();
        assertNull(twitter.username);
        assertNull(twitter.token);
        assertNull(twitter.secret);
        assertEquals(22, twitter.x);
        assertEquals(23, twitter.y);
    }

    @Test
    public void testDeleteWorksForPreviewApp() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        clientPair.appClient.verifyResult(ok(2));

        QrHolder[] qrHolders = makeQRs(devices, 1);

        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.staticMailBody.replace("{project_name}", "My Dashboard").replace(DYNAMIC_SECTION, sb.toString())), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].token + " " + qrHolders[0].dashId + " " + getUserName());

        DashBoard dashBoard = clientPair.appClient.parseDash(3);
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
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.deleteDash(2);
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("loadProfileGzipped 1");
        dashBoard = clientPair.appClient.parseDash(6);
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);

        clientPair.appClient.send("loadProfileGzipped 2");
        clientPair.appClient.verifyResult(illegalCommand(7));
    }

    @Test
    public void testDeleteWorksForParentOfPreviewApp() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        clientPair.appClient.verifyResult(ok(2));

        QrHolder[] qrHolders = makeQRs(devices, 1);

        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(holder.mailWrapper, timeout(500)).sendWithAttachment(eq(getUserName()), eq("AppPreview" + " - App details"), eq(holder.textHolder.staticMailBody.replace("{project_name}", "My Dashboard").replace(DYNAMIC_SECTION, sb.toString())), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].token + " " + qrHolders[0].dashId + " " + getUserName());

        DashBoard dashBoard = clientPair.appClient.parseDash(3);
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
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.deleteDash(1);
        clientPair.appClient.verifyResult(ok(5));
        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        assertNotNull(profile);
        assertNotNull(profile.dashBoards);
        assertEquals(1, profile.dashBoards.length);

        clientPair.appClient.send("loadProfileGzipped 2");
        String response = clientPair.appClient.getBody(2);
        assertNotNull(response);
    }

    @Test
    public void testExportedAppFlowWithOneDynamicTest() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(2));

        appClient2.send("loadProfileGzipped 1");
        DashBoard dashBoard = appClient2.parseDash(3);
        assertNotNull(dashBoard);

        Device device = dashBoard.devices[0];
        assertNotNull(device);
        assertNotNull(device.token);

        TestHardClient hardClient1 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient1.start();

        hardClient1.login(device.token);
        hardClient1.verifyResult(ok(1));
        appClient2.verifyResult(hardwareConnected(1, "1-0"));

        hardClient1.send("hardware vw 1 100");
        appClient2.verifyResult(hardware(2, "1-0 vw 1 100"));
    }

    @Test
    public void testFullDynamicAppFlow() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"My App\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = clientPair.appClient.parseApp(1);
        assertNotNull(app);
        assertNotNull(app.id);

        clientPair.hardwareClient.send("hardware dw 1 abc");
        clientPair.hardwareClient.send("hardware vw 77 123");

        clientPair.appClient.verifyResult(hardware(1, "1-0 dw 1 abc"));
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 77 123"));

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = clientPair.appClient.parseDash(4);
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.pinsStorage);
        assertEquals(0, dashBoard.pinsStorage.size());
        Widget w = dashBoard.findWidgetByPin(0, (short) 1, PinType.DIGITAL);
        assertNotNull(w);
        assertEquals("abc", ((OnePinWidget) w).value);

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        clientPair.appClient.verifyResult(ok(2));

        QrHolder[] qrHolders = makeQRs(devices, 1);

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(2));

        appClient2.send("loadProfileGzipped " + qrHolders[0].token + "\0" + 1 + "\0" + getUserName() + "\0" + AppNameUtil.BLYNK);
        dashBoard = appClient2.parseDash(3);
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.pinsStorage);
        assertTrue(dashBoard.pinsStorage.isEmpty());
        w = dashBoard.findWidgetByPin(0, (short) 1, PinType.DIGITAL);
        assertNotNull(w);
        assertNull(((OnePinWidget) w).value);

        Device device = dashBoard.devices[0];
        assertNotNull(device);
        assertNull(device.token);

        appClient2.reset();
        appClient2.getDevice(1, device.id);
        device = appClient2.parseDevice();

        TestHardClient hardClient1 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient1.start();

        hardClient1.login(device.token);
        hardClient1.verifyResult(ok(1));
        appClient2.verifyResult(hardwareConnected(1, "1-0"));

        hardClient1.send("hardware vw 1 100");
        appClient2.verifyResult(hardware(2, "1-0 vw 1 100"));
    }

    @Test
    public void testTimeInputInTheFaceAndDeviceTilesIsNotErasedByParentFaceUpdate() throws Exception {
        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";

        clientPair.appClient.createDash(dashBoard);

        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        clientPair.appClient.createDevice(dashBoard.id, device0);
        device0 = clientPair.appClient.parseDevice(2);
        clientPair.appClient.verifyResult(createDevice(2, device0));

        Device device2 = new Device(2, "My Dashboard", BoardType.Arduino_UNO);
        clientPair.appClient.createDevice(dashBoard.id, device2);
        device2 = clientPair.appClient.parseDevice(3);
        clientPair.appClient.verifyResult(createDevice(3, device2));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = clientPair.appClient.parseApp(4);
        assertNotNull(app);
        assertNotNull(app.id);


        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        //creating manually widget for child project
        clientPair.appClient.createWidget(dashBoard.id, deviceTiles);
        clientPair.appClient.verifyResult(ok(5));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                                                         null, new int[] {2}, "123", "name", "iconName", BoardType.ESP8266, null,
                                                         false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.send("createTemplate " + b("10 " + deviceTiles.id + " ")
                                          + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(6));

        TimeInput timeInput = new TimeInput();
        timeInput.width = 2;
        timeInput.height = 1;
        timeInput.id = 333;
        timeInput.pin = 77;
        timeInput.pinType = PinType.VIRTUAL;
        clientPair.appClient.createWidget(dashBoard.id, deviceTiles.id, tileTemplate.id, timeInput);
        clientPair.appClient.verifyResult(ok(7));

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.register("test@blynk.cc", "a", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(2));

        appClient2.send("loadProfileGzipped");
        Profile profile = appClient2.parseProfile(3);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(1, dashBoard.widgets.length);
        assertTrue(dashBoard.widgets[0] instanceof DeviceTiles);
        deviceTiles = (DeviceTiles) dashBoard.getWidgetById(deviceTiles.id);
        assertNotNull(deviceTiles.tiles);
        assertNotNull(deviceTiles.templates);
        assertEquals(0, deviceTiles.tiles.length);
        assertEquals(1, deviceTiles.templates.length);
        timeInput = (TimeInput) deviceTiles.getWidgetById(timeInput.id);
        assertNotNull(timeInput);
        assertNull(timeInput.value);

        Device provisionedDevice = new Device();
        provisionedDevice.id = 0;
        provisionedDevice.name = "123";
        provisionedDevice.boardType = BoardType.ESP8266;
        appClient2.createDevice(1, provisionedDevice);
        provisionedDevice = appClient2.parseDevice(4);
        assertNotNull(provisionedDevice);

        appClient2.send("hardware 1-0 vw " + b("77 82800 82860 Europe/Kiev 1"));

        appClient2.send("loadProfileGzipped");
        profile = appClient2.parseProfile(6);
        assertEquals(1, profile.dashBoards.length);
        dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
        assertEquals(1, dashBoard.parentId);
        assertEquals(1, dashBoard.widgets.length);
        assertTrue(dashBoard.widgets[0] instanceof DeviceTiles);
        deviceTiles = (DeviceTiles) dashBoard.getWidgetById(deviceTiles.id);
        assertNotNull(deviceTiles.tiles);
        assertNotNull(deviceTiles.templates);
        assertEquals(0, deviceTiles.tiles.length);
        assertEquals(1, deviceTiles.templates.length);
        timeInput = (TimeInput) deviceTiles.getWidgetById(timeInput.id);
        assertNotNull(timeInput);
        assertNull(timeInput.value);

        appClient2.sync(dashBoard.id, provisionedDevice.id);
        appClient2.verifyResult(ok(7));
        appClient2.verifyResult(appSync(1111, "1-0 vw 77 82800 82860 Europe/Kiev 1"));

        clientPair.appClient.send("updateFace 1");
        clientPair.appClient.verifyResult(ok(8));

        if (!appClient2.isClosed()) {
            sleep(300);
        }
        assertTrue(appClient2.isClosed());

        appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.login("test@blynk.cc", "a", "Android", "1.10.4", app.id);
        appClient2.verifyResult(ok(1));

        appClient2.sync(dashBoard.id, provisionedDevice.id);
        appClient2.verifyResult(ok(2));
        appClient2.verifyResult(appSync(1111, "1-0 vw 77 82800 82860 Europe/Kiev 1"));
    }

    private QrHolder[] makeQRs(Device[] devices, int dashId) throws Exception {
        QrHolder[] qrHolders = new QrHolder[devices.length];

        List<FlashedToken> flashedTokens = getAllTokens();

        int i = 0;
        for (Device device : devices) {
            String newToken = flashedTokens.get(i).token;
            qrHolders[i] = new QrHolder(dashId, device.id, device.name, newToken, QRCode.from(newToken).to(ImageType.JPG).stream().toByteArray());
            i++;
        }

        return qrHolders;
    }

    private FlashedToken getFlashedTokenByDevice() throws Exception {
        List<FlashedToken> flashedTokens = getAllTokens();

        int i = 0;
        for (FlashedToken flashedToken : flashedTokens) {
            if (-1 == flashedToken.deviceId) {
                return flashedTokens.get(i);
            }

        }
        return null;
    }

    private List<FlashedToken> getAllTokens() throws Exception {
        List<FlashedToken> list = new ArrayList<>();
        try (Connection connection = holder.dbManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("select * from flashed_tokens")) {

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
