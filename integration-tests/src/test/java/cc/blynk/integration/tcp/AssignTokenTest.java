package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTestWithDB;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.integration.model.tcp.TestSslHardClient;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.db.model.FlashedToken;
import cc.blynk.utils.AppNameUtil;
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
public class AssignTokenTest extends SingleServerInstancePerTestWithDB {

    @Before
    public void cleanTable() throws Exception {
        holder.dbManager.executeSQL("DELETE FROM flashed_tokens");
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
        holder.dbManager.insertFlashedTokens(list);

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
        holder.dbManager.insertFlashedTokens(list);

        clientPair.appClient.send("assignToken 1\0" + flashedToken.token);
        clientPair.appClient.verifyResult(ok(1));

        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        hardClient2.login(flashedToken.token);
        hardClient2.verifyResult(ok(1));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices(3);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        assertEquals(flashedToken.token, devices[0].token);
        assertEquals(flashedToken.deviceId, devices[0].id);
        assertEquals(Status.ONLINE, devices[0].status);
    }

    @Test
    public void testConnectTo443PortForHardware() throws Exception {
        clientPair.appClient.createDevice(1, new Device(1, "My Device", BoardType.ESP8266));
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);

        TestSslHardClient hardClient2 = new TestSslHardClient("localhost", properties.getHttpsPort());
        hardClient2.start();

        hardClient2.login(device.token);
        hardClient2.verifyResult(ok(1));
    }
}
