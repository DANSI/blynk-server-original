package cc.blynk.integration.tcp;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.Holder;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.AppNameUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AssignTokenTest extends BaseTest {

    private DBManager dbManager;
    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        Holder holder = new Holder(properties, twitterWrapper, mailWrapper,
                gcmWrapper, smsWrapper, slackWrapper, "db-test.properties");
        hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        appServer = new AppAndHttpsServer(holder).start();
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
        clientPair.appClient.send("assignToken 1\0" + "123");
        clientPair.appClient.verifyResult(notAllowed(1));
    }

    @Test
    public void testTokenActivate() throws Exception {
        FlashedToken[] list = new FlashedToken[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        FlashedToken flashedToken = new FlashedToken("test@blynk.cc", token, AppNameUtil.BLYNK, 1, 0);
        list[0] = flashedToken;
        dbManager.insertFlashedTokens(list);

        clientPair.appClient.send("assignToken 1\0" + flashedToken.token);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("assignToken 1\0" + flashedToken.token);
        clientPair.appClient.verifyResult(notAllowed(2));
    }

    @Test
    public void testCorrectToken() throws Exception {
        FlashedToken[] list = new FlashedToken[1];
        String token = UUID.randomUUID().toString().replace("-", "");
        FlashedToken flashedToken = new FlashedToken("test@blynk.cc", token, AppNameUtil.BLYNK, 1, 0);
        list[0] = flashedToken;
        dbManager.insertFlashedTokens(list);

        clientPair.appClient.send("assignToken 1\0" + flashedToken.token);
        clientPair.appClient.verifyResult(ok(1));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.login(flashedToken.token);
        hardClient2.verifyResult(ok(1));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.getDevices(3);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        assertEquals(flashedToken.token, devices[0].token);
        assertEquals(flashedToken.deviceId, devices[0].id);
        assertEquals(Status.ONLINE, devices[0].status);
    }


}
