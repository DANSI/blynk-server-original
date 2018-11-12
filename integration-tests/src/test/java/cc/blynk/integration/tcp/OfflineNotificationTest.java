package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DashboardSettings;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.deviceOffline;
import static cc.blynk.integration.TestUtil.ok;
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
public class OfflineNotificationTest extends SingleServerInstancePerTest {

    private static int tcpHardPort;

    @BeforeClass
    public static void initPort() {
        tcpHardPort = properties.getHttpPort();
    }

    @Override
    protected String changeProfileTo() {
        return "user_profile_json_empty_dash.txt";
    }

    @Test
    public void testOfflineTimingIsCorrectForMultipleDevices() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices(2);
        assertNotNull(devices);
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
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices(2);
        assertNotNull(devices);
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
        DashboardSettings settings = new DashboardSettings("New Name",
                true, Theme.BlynkLight, true, true, true, false, 0, false);

        clientPair.appClient.send("updateSettings 1\0" + JsonParser.toJson(settings));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);
        DashBoard dashBoard = profile.dashBoards[0];
        assertNotNull(dashBoard);
        assertEquals(settings.name, dashBoard.name);
        assertEquals(settings.isAppConnectedOn, dashBoard.isAppConnectedOn);
        assertEquals(settings.isNotificationsOff, dashBoard.isNotificationsOff);
        assertTrue(dashBoard.isNotificationsOff);
        assertEquals(settings.isShared, dashBoard.isShared);
        assertEquals(settings.keepScreenOn, dashBoard.keepScreenOn);
        assertEquals(settings.theme, dashBoard.theme);
        assertEquals(settings.widgetBackgroundOn, dashBoard.widgetBackgroundOn);

        clientPair.hardwareClient.stop();

        clientPair.appClient.neverAfter(500, deviceOffline(0, "1-0"));

        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        Device device = clientPair.appClient.parseDevice(3);
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(3, device));

        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices(4);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);

        settings = new DashboardSettings("New Name",
                true, Theme.BlynkLight, true, true, false, false, 0, false);
        clientPair.appClient.send("updateSettings 1\0" + JsonParser.toJson(settings));
        clientPair.appClient.verifyResult(ok(5));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));
        hardClient2.stop();

        clientPair.appClient.verifyResult(deviceOffline(0, "1-1"));
    }

    @Test
    public void testTurnOffNotificationsAndNoDevices() throws Exception{
        DashboardSettings settings = new DashboardSettings("New Name",
                true, Theme.BlynkLight, true, true, true, false, 0, false);

        clientPair.appClient.send("updateSettings 1\0" + JsonParser.toJson(settings));
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.stop();
        clientPair.appClient.neverAfter(500, deviceOffline(0, "1-0"));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(2));
    }

    @Test
    public void deviceGoesOfflineAfterBeingIdle() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices(2);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));

        hardClient2.send("internal " + b("ver 0.3.1 h-beat 1 buff-in 256 dev Arduino cpu ATmega328P con W5100"));
        hardClient2.verifyResult(ok(2));

        //just waiting 2.5 secs so server trigger idle event
        TestUtil.sleep(2500);

        clientPair.appClient.verifyResult(deviceOffline(0, b("1-1")));
    }

    @Test
    public void sessionDisconnectChangeState() throws Exception {
        Device device2 = new Device(1, "My Device", BoardType.ESP8266);
        device2.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device2);
        clientPair.appClient.send("getDevices 1");

        Device[] devices = clientPair.appClient.parseDevices(2);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEquals(1, devices[1].id);
        assertEquals(0, devices[1].disconnectTime);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));

        holder.sessionDao.close();

        TestAppClient testAppClient = new TestAppClient(properties);
        testAppClient.start();
        testAppClient.login(getUserName(), "1");
        testAppClient.verifyResult(ok(1));

        testAppClient.send("getDevices 1");
        devices = testAppClient.parseDevices(2);
        assertNotNull(devices);
        assertEquals(2, devices.length);
        assertEquals(1, devices[1].id);
        assertEquals(System.currentTimeMillis(), devices[1].disconnectTime, 5000);
    }
}
