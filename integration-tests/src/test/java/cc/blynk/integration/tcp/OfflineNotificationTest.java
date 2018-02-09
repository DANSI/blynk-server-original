package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DashboardSettings;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareServer;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class OfflineNotificationTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppAndHttpsServer(holder).start();

        this.clientPair = initAppAndHardPair("user_profile_json_empty_dash.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testOfflineTimingIsCorrectForMultipleDevices() throws Exception {
        Device device2 = new Device(1, "My Device", "ESP8266");
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        Device device = clientPair.appClient.getDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.getDevices(2);
        TestCase.assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));

        hardClient2.send("internal " + b("ver 0.3.1 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100"));
        hardClient2.verifyResult(ok(2));

        clientPair.hardwareClient.stop();

        clientPair.appClient.verifyResult(deviceOffline(0, "1-0"));
        clientPair.appClient.never(deviceOffline(0, "1-1"));

        clientPair.appClient.reset();
        hardClient2.stop();

        clientPair.appClient.verifyResult(deviceOffline(0, b("1-1")));
        clientPair.appClient.never(deviceOffline(0, b("1-0")));
    }

    @Test
    public void testOfflineTimingIsCorrectForMultipleDevices2() throws Exception {
        Device device2 = new Device(1, "My Device", "ESP8266");
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        Device device = clientPair.appClient.getDevice();
        TestCase.assertNotNull(device);
        TestCase.assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.getDevices(2);
        TestCase.assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));

        hardClient2.send("internal " + b("ver 0.3.1 h-beat 1 buff-in 256 dev Arduino cpu ATmega328P con W5100"));
        hardClient2.verifyResult(ok(2));

        clientPair.hardwareClient.stop();
        hardClient2.stop();

        clientPair.appClient.verifyResult(deviceOffline(0, b("1-0")));
        clientPair.appClient.verifyResult(deviceOffline(0, b("1-1")));
    }

    @Test
    public void testTurnOffNotifications() throws Exception{
        DashboardSettings settings = new DashboardSettings("New Name", true, Theme.BlynkLight, true, true, true);

        clientPair.appClient.send("updateSettings 1\0" + JsonParser.toJson(settings));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.getProfile(2);
        DashBoard dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(settings.name, dashBoard.name);
        assertEquals(settings.isAppConnectedOn, dashBoard.isAppConnectedOn);
        assertEquals(settings.isNotificationsOff, dashBoard.isNotificationsOff);
        assertTrue(dashBoard.isNotificationsOff);
        assertEquals(settings.isShared, dashBoard.isShared);
        assertEquals(settings.keepScreenOn, dashBoard.keepScreenOn);
        assertEquals(settings.theme, dashBoard.theme);

        clientPair.hardwareClient.stop();

        clientPair.appClient.neverAfter(500, deviceOffline(0, "1-0"));

        Device device2 = new Device(1, "My Device", "ESP8266");
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        Device device = clientPair.appClient.getDevice(3);
        TestCase.assertNotNull(device);
        TestCase.assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(3, device));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.getDevices(4);
        TestCase.assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);

        settings = new DashboardSettings("New Name", true, Theme.BlynkLight, true, true, false);
        clientPair.appClient.send("updateSettings 1\0" + JsonParser.toJson(settings));
        clientPair.appClient.verifyResult(ok(5));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));
        hardClient2.stop();

        clientPair.appClient.verifyResult(deviceOffline(0, "1-1"));
    }
}
