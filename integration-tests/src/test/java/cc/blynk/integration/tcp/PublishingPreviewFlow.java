package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
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
    public void testSendStaticEmailForAppPublish() throws Exception {
        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("email 1 Blynk STATIC 123123 AppPreview");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolderTest[] qrHolders = makeQRs(DEFAULT_TEST_USER, devices, 1);

        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{device_section}", "<br>" + qrHolders[0].mailBodyPart)), eq(qrHolders));
    }

    @Test
    public void testSenddynamicEmailForAppPublish() throws Exception {
        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("email 1 Blynk DYNAMIC 123123 AppPreview");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        verify(mailWrapper, timeout(500)).sendHtml(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.DYNAMIC_MAIL_BODY));
    }

    @Test
    public void testDeleteWorksForPreviewApp() throws Exception {
        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("email 1 Blynk STATIC 123123 AppPreview");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        QrHolderTest[] qrHolders = makeQRs(DEFAULT_TEST_USER, devices, 1);

        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{device_section}", "<br>" + qrHolders[0].mailBodyPart)), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].code);

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

        clientPair.appClient.send("deleteDash 1" + "\0" + "child");
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
        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertEquals(1, devices.length);

        clientPair.appClient.send("email 1 Blynk STATIC 123123 AppPreview");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        QrHolderTest[] qrHolders = makeQRs(DEFAULT_TEST_USER, devices, 1);

        verify(mailWrapper, timeout(500)).sendWithAttachment(eq(DEFAULT_TEST_USER), eq("AppPreview" + " - App details"), eq(holder.limits.STATIC_MAIL_BODY.replace("{device_section}", "<br>" + qrHolders[0].mailBodyPart)), eq(qrHolders));

        clientPair.appClient.send("loadProfileGzipped " + qrHolders[0].code);

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
        assertEquals(0, profile.dashBoards.length);

        clientPair.appClient.send("loadProfileGzipped 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(7, ILLEGAL_COMMAND)));
    }

    private QrHolderTest[] makeQRs(String to, Device[] devices, int dashId) throws Exception {
        QrHolderTest[] qrHolders = new QrHolderTest[devices.length];

        List<FlashedToken> flashedTokens = getAllTokens();

        int i = 0;
        for (Device device : devices) {
            String newToken = flashedTokens.get(i).token;
            String name = newToken + "_" + dashId + "_" + device.id + ".jpg";
            String qrCode = newToken + " " + dashId + " " + to;
            String mailBodyPart = device.name + ": " + newToken;
            qrHolders[i] = new QrHolderTest(name, mailBodyPart, QRCode.from(qrCode).to(ImageType.JPG).stream().toByteArray(), qrCode);
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
                        rs.getString("email"), rs.getInt("device_id"),
                        rs.getBoolean("is_activated"), rs.getDate("ts")
                ));
            }
            connection.commit();
        }
        return list;
    }

    private static class QrHolderTest extends QrHolder {

        String code;

        public QrHolderTest(String name, String mailBodyPart, byte[] data, String code) {
            super(name, mailBodyPart, data);
            this.code = code;
        }
    }
}
