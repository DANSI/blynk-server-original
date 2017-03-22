package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.Holder;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static cc.blynk.server.core.protocol.enums.Response.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AssignTokenTest extends IntegrationBase {

    private DBManager dbManager;
    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        Holder holder = new Holder(properties, twitterWrapper, mailWrapper, gcmWrapper, smsWrapper, "db-test.properties");
        hardwareServer = new HardwareServer(holder).start();
        appServer = new AppServer(holder).start();
        dbManager = holder.dbManager;

        this.clientPair = initAppAndHardPair();
        assertNotNull(dbManager.getConnection());
        dbManager.executeSQL("DELETE FROM flashed_tokens");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
        dbManager.close();
    }

    @Test
    public void testNoTokenExists() throws Exception {
        clientPair.appClient.send("getToken 1\0" + "123");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(1)));
    }

    @Test
    public void testTokenActivate() throws Exception {
        FlashedToken[] list = new FlashedToken[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        FlashedToken flashedToken = new FlashedToken(token, AppName.BLYNK, 0);
        list[0] = flashedToken;
        dbManager.insertFlashedTokens(list);

        clientPair.appClient.send("getToken 1\0" + flashedToken.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1\0" + flashedToken.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(2)));
    }

    @Test
    public void testCorrectToken() throws Exception {
        FlashedToken[] list = new FlashedToken[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        FlashedToken flashedToken = new FlashedToken(token, AppName.BLYNK, 0);
        list[0] = flashedToken;
        dbManager.insertFlashedTokens(list);

        clientPair.appClient.send("getToken 1\0" + flashedToken.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + flashedToken.token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("getDevices 1");

        String response = clientPair.appClient.getBody(3);

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        assertEquals(flashedToken.token, devices[0].token);
        assertEquals(flashedToken.deviceId, devices[0].id);
        assertEquals(Status.ONLINE, devices[0].status);
    }


}
