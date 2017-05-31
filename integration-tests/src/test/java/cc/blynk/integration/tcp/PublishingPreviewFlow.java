package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.server.Holder;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.CreateDevice;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.server.notifications.mail.QrHolder;
import cc.blynk.utils.JsonParser;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
        this.appServer = new AppServer(holder).start();

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
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

        clientPair.appClient.send("getProjectByToken " + qrHolders[0].token);
        String body = clientPair.appClient.getBody(3);
        assertNotNull(body);
        DashBoard dashBoard = JsonParser.parseDashboard(body);
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);
    }

    @Test
    public void testSendStaticEmailForAppPublish() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

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
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);

        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.DYNAMIC_MAIL_BODY.replace("{project_name}", "My Dashboard")), eq(qrHolders));
    }

    @Test
    public void testSendDynamicEmailForAppPublishWithFewDevices() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        device1 = JsonParser.parseDevice(clientPair.appClient.getBody(1));
        assertNotNull(device1);
        assertEquals(1, device1.id);

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"DYNAMIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(2));
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(2, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, true);

        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.DYNAMIC_MAIL_BODY.replace("{project_name}", "My Dashboard")), eq(qrHolders));
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

        clientPair.appClient.send("createDash " + dashBoard.toString());

        Device device0 = new Device(0, "My Dashboard", "UNO");
        device0.status = Status.ONLINE;

        clientPair.appClient.send("createDevice 10\0" + device0.toString());
        String createdDevice = clientPair.appClient.getBody(2);
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(2, device.toString())));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(3));
        assertNotNull(app);
        assertNotNull(app.id);


        clientPair.appClient.send("emailQr 10\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(4)));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{project_name}", "Face Edit Test").replace("{device_section}", sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.send("register test@blynk.cc a " + app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.send("login test@blynk.cc a Android 1.10.4 " + app.id);
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
    public void testFaceEditForRestrictiveFields() throws Exception {
        Profile profile = JsonParser.parseProfileFromString(readTestUserProfile());

        DashBoard dashBoard = profile.dashBoards[0];
        dashBoard.id = 10;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;
        dashBoard.name = "Face Edit Test";
        dashBoard.devices = null;

        clientPair.appClient.send("createDash " + dashBoard.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        Device device0 = new Device(0, "My Device", "UNO");
        device0.status = Status.ONLINE;

        clientPair.appClient.send("createDevice 10\0" + device0.toString());
        String createdDevice = clientPair.appClient.getBody(2);
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(2, device.toString())));

        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[10]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody(3));
        assertNotNull(app);
        assertNotNull(app.id);


        clientPair.appClient.send("emailQr 10\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(4)));

        QrHolder[] qrHolders = makeQRs(new Device[] {device}, 10, false);
        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{project_name}", "Face Edit Test").replace("{device_section}", sb.toString())), eq(qrHolders));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();

        appClient2.send("register test@blynk.cc a " + app.id);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        appClient2.send("login test@blynk.cc a Android 1.10.4 " + app.id);
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

        clientPair.appClient.send("updateWidget 1\0" + "{\"id\":10, \"height\":2, \"width\":1, \"x\":22, \"y\":23, \"username\":\"pupkin@gmail.com\", \"token\":\"token\", \"secret\":\"secret\", \"type\":\"TWITTER\"}");
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
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);

        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].token + " " + qrHolders[0].dashId + " " + DEFAULT_TEST_USER);

        DashBoard dashBoard = JsonParser.parseDashboard(clientPair.appClient.getBody(3));
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.devices);
        assertNull(dashBoard.devices[0].token);
        assertNull(dashBoard.devices[0].lastLoggedIP);
        assertEquals(0, dashBoard.devices[0].disconnectTime);
        assertEquals(Status.OFFLINE, dashBoard.devices[0].status);

        dashBoard.id = 2;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;

        clientPair.appClient.send("createDash " + dashBoard.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.appClient.send("deleteDash 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("loadProfileGzipped 1");
        dashBoard = JsonParser.parseDashboard(clientPair.appClient.getBody(6));
        assertNotNull(dashBoard);
        assertEquals(1, dashBoard.id);

        clientPair.appClient.send("loadProfileGzipped 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(7, ILLEGAL_COMMAND)));
    }

    @Test
    public void testDeleteWorksForParentOfPreviewApp() throws Exception {
        clientPair.appClient.send("createApp {\"theme\":\"Blynk\",\"provisionType\":\"STATIC\",\"color\":0,\"name\":\"AppPreview\",\"icon\":\"myIcon\",\"projectIds\":[1]}");
        App app = JsonParser.parseApp(clientPair.appClient.getBody());
        assertNotNull(app);
        assertNotNull(app.id);
        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("emailQr 1\0" + app.id);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        QrHolder[] qrHolders = makeQRs(devices, 1, false);

        StringBuilder sb = new StringBuilder();
        qrHolders[0].attach(sb);
        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{project_name}", "My Dashboard").replace("{device_section}", sb.toString())), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].token + " " + qrHolders[0].dashId + " " + DEFAULT_TEST_USER);

        DashBoard dashBoard = JsonParser.parseDashboard(clientPair.appClient.getBody(3));
        assertNotNull(dashBoard);
        assertNotNull(dashBoard.devices);
        assertNull(dashBoard.devices[0].token);
        assertNull(dashBoard.devices[0].lastLoggedIP);
        assertEquals(0, dashBoard.devices[0].disconnectTime);
        assertEquals(Status.OFFLINE, dashBoard.devices[0].status);

        dashBoard.id = 2;
        dashBoard.parentId = 1;
        dashBoard.isPreview = true;

        clientPair.appClient.send("createDash " + dashBoard.toString());
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
