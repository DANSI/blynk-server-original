package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.controls.Terminal;
import cc.blynk.server.core.model.widgets.outputs.LCD;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.table.Table;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.appSync;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.hardwareConnected;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.setProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DeviceSelectorWorkflowTest extends SingleServerInstancePerTest {

    private static int tcpHardPort;

    @BeforeClass
    public static void initPort() {
        tcpHardPort = properties.getHttpPort();
    }

    private static void assertEqualDevice(Device expected, Device real) {
        assertEquals(expected.id, real.id);
        //assertEquals(expected.name, real.name);
        assertEquals(expected.boardType, real.boardType);
        assertNotNull(real.token);
        assertEquals(expected.status, real.status);
    }

    @Test
    public void testSendHardwareCommandViaDeviceSelector() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));
        device1.status = Status.ONLINE;

        clientPair.appClient.send("hardware 1-200000 vw 88 1");
        clientPair.hardwareClient.verifyResult(hardware(2, "vw 88 1"));
        hardClient2.never(hardware(2, "vw 88 1"));

        //change device
        clientPair.appClient.send("hardware 1 vu 200000 1");
        clientPair.appClient.verifyResult(ok(3));
        clientPair.hardwareClient.never(hardware(3, "vu 200000 1"));
        hardClient2.never(hardware(3, "vu 200000 1"));

        clientPair.appClient.send("hardware 1-200000 vw 88 2");
        clientPair.hardwareClient.never(hardware(4, "vw 88 2"));
        hardClient2.verifyResult(hardware(4, "vw 88 2"));

        //change device back
        clientPair.appClient.send("hardware 1 vu 200000 0");
        clientPair.appClient.verifyResult(ok(5));
        clientPair.hardwareClient.never(hardware(5, "vu 200000 0"));
        hardClient2.never(hardware(5, "vu 200000 0"));
        clientPair.appClient.verifyResult(appSync(1111, b("1-0 vw 88 1")));

        clientPair.appClient.send("hardware 1-200000 vw 88 0");
        clientPair.hardwareClient.verifyResult(hardware(6, "vw 88 0"));
        hardClient2.never(hardware(6, "vw 88 0"));
    }

    @Test
    public void testSendHardwareCommandViaDeviceSelectorInSharedApp() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getShareToken 1");

        String sharedToken = clientPair.appClient.getBody(4);
        assertNotNull(sharedToken);
        assertEquals(32, sharedToken.length());

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));
        device1.status = Status.ONLINE;
        clientPair.appClient.verifyResult(hardwareConnected(1, "1-1"));


        //login with shared app
        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + sharedToken + " Android 24");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient2.send("hardware 1-200000 vw 88 1");
        clientPair.hardwareClient.verifyResult(hardware(2, "vw 88 1"));
        hardClient2.never(hardware(2, "vw 88 1"));
        clientPair.appClient.verifyResult(appSync(2, b("1-200000 vw 88 1")));

        clientPair.hardwareClient.send("hardware vw 88 value_from_device_0");
        hardClient2.send("hardware vw 88 value_from_device_1");

        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 88 value_from_device_0"));
        clientPair.appClient.verifyResult(hardware(2, "1-1 vw 88 value_from_device_1"));

        appClient2.verifyResult(hardware(1, "1-0 vw 88 value_from_device_0"));
        appClient2.verifyResult(hardware(2, "1-1 vw 88 value_from_device_1"));

        //change device
        appClient2.send("hardware 1 vu 200000 1");
        appClient2.verifyResult(ok(3));
        clientPair.hardwareClient.never(hardware(3, "vu 200000 1"));
        hardClient2.never(hardware(3, "vu 200000 1"));
        clientPair.appClient.verifyResult(appSync(3, b("1 vu 200000 1")));
        clientPair.appClient.verifyResult(appSync(b("1-1 vw 88 value_from_device_1")));

        appClient2.send("hardware 1-200000 vw 88 2");
        clientPair.hardwareClient.never(hardware(4, "vw 88 2"));
        hardClient2.verifyResult(hardware(4, "vw 88 2"));
        clientPair.appClient.verifyResult(appSync(4, b("1-200000 vw 88 2")));

        //change device back
        appClient2.send("hardware 1 vu 200000 0");
        appClient2.verifyResult(ok(5));
        clientPair.hardwareClient.never(hardware(5, "vu 200000 0"));
        hardClient2.never(hardware(5, "vu 200000 0"));
        clientPair.appClient.verifyResult(appSync(5, b("1 vu 200000 0")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 88 value_from_device_0")));

        appClient2.send("hardware 1-200000 vw 88 0");
        clientPair.hardwareClient.verifyResult(hardware(6, "vw 88 0"));
        hardClient2.never(hardware(6, "vw 88 0"));
        clientPair.appClient.verifyResult(appSync(6, b("1-200000 vw 88 0")));
    }

    @Test
    public void testSetPropertyIsSentForDeviceSelectorWidget() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.createWidget(1, "{\"id\":89, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Display\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":89}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        clientPair.hardwareClient.setProperty(89, "label", "123");
        clientPair.appClient.verifyResult(setProperty(1, "1-0 89 label 123"));
    }

    @Test
    public void testSetPropertyIsSentForDeviceSelectorWidgetOnActivateForExistingWidget() throws Exception {
        testSetPropertyIsSentForDeviceSelectorWidget();

        clientPair.hardwareClient.send("hardware vw 89 1");
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 89 1"));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(setProperty(1111, "1-0 89 label 123"));
        clientPair.appClient.verifyResult(appSync(1111, b("1-0 vw 89 1")));
    }

    @Test
    public void testSetPropertyIsRememberedBetweenDevices() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        clientPair.hardwareClient.setProperty(88, "label", "123");
        clientPair.appClient.verifyResult(setProperty(1, "1-0 88 label 123"));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));
        clientPair.appClient.verifyResult(hardwareConnected(1, "1-1"));

        hardClient2.setProperty(88, "label", "124");
        clientPair.appClient.verifyResult(setProperty(2, "1-1 88 label 124"));

        clientPair.appClient.send("hardware 1 vu 200000 1");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(setProperty(1111, "1-1 88 label 124"));
    }

    @Test
    public void testBasicSelectorWorkflow() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();

        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.createWidget(1, "{\"id\":89, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Display\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":89}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();

        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));
        clientPair.appClient.verifyResult(hardwareConnected(1, "1-1"));
        device1.status = Status.ONLINE;

        clientPair.hardwareClient.send("hardware vw 89 value_from_device_0");
        hardClient2.send("hardware vw 89 value_from_device_1");

        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 89 value_from_device_0"));
        clientPair.appClient.verifyResult(hardware(2, "1-1 vw 89 value_from_device_1"));

        clientPair.appClient.send("hardware 1 vw 88 100");

        clientPair.hardwareClient.verifyResult(hardware(2, "vw 88 100"));

        //change device, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 1");
        clientPair.appClient.verifyResult(ok(3));
        clientPair.hardwareClient.never(hardware(3, "vu 200000 1"));
        hardClient2.never(hardware(3, "vu 200000 1"));

        clientPair.appClient.verifyResult(ok(3));
        clientPair.appClient.verifyResult(appSync(b("1-1 vw 89 value_from_device_1")));

        //switch device back, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 0");
        clientPair.appClient.verifyResult(ok(4));
        clientPair.hardwareClient.never(hardware(4, "vu 200000 0"));
        hardClient2.never(hardware(4, "vu 200000 0"));

        clientPair.appClient.verifyResult(appSync(b("1-0 vw 89 value_from_device_0")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 88 100")));
    }

    @Test
    public void testDeviceSelectorSyncTimeInput() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();

        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"type\":\"TIME_INPUT\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1, \"deviceId\":200000}");

        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();

        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));
        clientPair.appClient.verifyResult(hardwareConnected(1, "1-1"));
        device1.status = Status.ONLINE;

        clientPair.appClient.send("hardware 1 vw " + b("99 82800 82860 Europe/Kiev 1"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 82800 82860 Europe/Kiev 1")));

        //change device, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 1");
        clientPair.appClient.verifyResult(ok(3));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(hardware(3, "vu 200000 1")));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(hardware(3, "vu 200000 1")));

        //switch device back, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 0");
        clientPair.appClient.verifyResult(ok(4));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(hardware(4, "vu 200000 0")));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(hardware(4, "vu 200000 0")));

        clientPair.appClient.verifyResult(ok(4));
        clientPair.appClient.verifyResult(appSync(b("1-0 dw 1 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 dw 2 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 3 0")));
        clientPair.appClient.verifyResult(appSync(b("1-0 dw 5 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 4 244")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 7 3")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 30 3")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 0 89.888037459418")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 11 -58.74774244674501")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 13 60 143 158")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 99 82800 82860 Europe/Kiev 1")));
    }

    @Test
    public void testNoSyncForDeviceSelectorWidget() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("hardware 1 vw 88 100");
        clientPair.hardwareClient.verifyResult(hardware(4, "vw 88 100"));

        clientPair.appClient.sync(1);
        verify(clientPair.appClient.responseMock, timeout(1000).times(15)).channelRead(any(), any());
        clientPair.appClient.verifyResult(ok(5));
        clientPair.appClient.never(appSync(b("1-200000 vw 88 100")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 88 100")));
    }

    @Test
    public void testDeviceSelectorWorksAfterDeviceRemoval() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"value\":0, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.createWidget(1, "{\"id\":89, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Display\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":89}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1-200000 vw 88 100");
        clientPair.hardwareClient.verifyResult(hardware(2, "vw 88 100"));

        //change device, expecting syncs and OK
        clientPair.appClient.send("hardware 1 vu 200000 1");
        clientPair.appClient.verifyResult(ok(3));
        clientPair.hardwareClient.never(hardware(3, "vu 200000 1"));

        clientPair.appClient.send("hardware 1-200000 vw 88 101");
        hardClient2.verifyResult(hardware(4, "vw 88 101"));

        clientPair.appClient.send("deleteDevice 1\0" + "1");
        clientPair.appClient.verifyResult(ok(5));

        //channel should be closed. so will not receive message
        clientPair.appClient.send("hardware 1-200000 vw 88 102");
        verify(hardClient2.responseMock, after(500).never()).channelRead(any(), eq(hardware(5, "vw 88 100")));
    }

    @Test
    public void terminalWithDeviceSelectorStoreMultipleCommands() throws Exception {
        var device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        var device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        var device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        var deviceSelector = new DeviceSelector();
        deviceSelector.id = 200000;
        deviceSelector.x = 0;
        deviceSelector.y = 0;
        deviceSelector.width = 1;
        deviceSelector.height = 1;
        deviceSelector.deviceIds = new int[] {0, 1};

        var terminal = new Terminal();
        terminal.id = 88;
        terminal.width = 1;
        terminal.height = 1;
        terminal.deviceId = (int) deviceSelector.id;
        terminal.pinType = PinType.VIRTUAL;
        terminal.pin = 88;

        clientPair.appClient.createWidget(1, deviceSelector);
        clientPair.appClient.createWidget(1, terminal);
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        for (var i = 1; i <= 26; i++) {
            clientPair.hardwareClient.send("hardware vw 88 " + i);
            clientPair.appClient.verifyResult(hardware(i, "1-0 vw 88 " + i));
        }

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        //expecting 25 syncs and not 26
        verify(clientPair.appClient.responseMock, timeout(1000).times(11 + 2)).channelRead(any(), any());

        for (var i = 2; i <= 26; i++) {
            clientPair.appClient.verifyResult(appSync("1-0 vm 88 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26"));
        }
    }

    @Test
    public void TableWithDeviceSelectorStoreMultipleCommands() throws Exception {
        var device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        var device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        var device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        var deviceSelector = new DeviceSelector();
        deviceSelector.id = 200000;
        deviceSelector.x = 0;
        deviceSelector.y = 0;
        deviceSelector.width = 1;
        deviceSelector.height = 1;
        deviceSelector.deviceIds = new int[] {0, 1};

        var table = new Table();
        table.id = 88;
        table.width = 1;
        table.height = 1;
        table.deviceId = (int) deviceSelector.id;
        table.pinType = PinType.VIRTUAL;
        table.pin = 88;

        clientPair.appClient.createWidget(1, deviceSelector);
        clientPair.appClient.createWidget(1, table);
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        for (var i = 1; i <= 11; i++) {
            clientPair.hardwareClient.send("hardware vw 88 " + i);
            clientPair.appClient.verifyResult(hardware(i, "1-0 vw 88 " + i));
        }

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        verify(clientPair.appClient.responseMock, timeout(1000).times(11 + 2)).channelRead(any(), any());

        clientPair.appClient.verifyResult(appSync("1-0 vm 88 1 2 3 4 5 6 7 8 9 10 11"));
    }

    @Test
    public void LCDWithDeviceSelectorStoreMultipleCommands() throws Exception {
        var device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        var device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        var device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        var deviceSelector = new DeviceSelector();
        deviceSelector.id = 200000;
        deviceSelector.x = 0;
        deviceSelector.y = 0;
        deviceSelector.width = 1;
        deviceSelector.height = 1;
        deviceSelector.deviceIds = new int[] {0, 1};

        var lcd = new LCD();
        lcd.id = 88;
        lcd.width = 1;
        lcd.height = 1;
        lcd.deviceId = (int) deviceSelector.id;
        lcd.dataStreams = new DataStream[] {new DataStream((short) 88, PinType.VIRTUAL)};

        clientPair.appClient.createWidget(1, deviceSelector);
        clientPair.appClient.createWidget(1, lcd);
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        for (var i = 1; i <= 7; i++) {
            clientPair.hardwareClient.send("hardware vw 88 " + i);
            clientPair.appClient.verifyResult(hardware(i, "1-0 vw 88 " + i));
        }

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        verify(clientPair.appClient.responseMock, timeout(1000).times(11 + 2)).channelRead(any(), any());

        clientPair.appClient.verifyResult(appSync("1-0 vm 88 2 3 4 5 6 7"));
    }

    @Test
    public void testDeviceSelectorForSharedApp() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.Arduino_UNO);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"STEP\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getShareToken 1");
        String token = clientPair.appClient.getBody(4);
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        //change device
        clientPair.appClient.send("hardware 1 vu 200000 1");
        clientPair.appClient.verifyResult(ok(5));
        clientPair.hardwareClient.never(hardware(5, "vu 200000 1"));
        appClient2.verifyResult(appSync(5, "1 vu 200000 1"));

        appClient2.send("hardware 1 vu 200000 0");
        appClient2.verifyResult(ok(2));
        clientPair.hardwareClient.never(hardware(2, "vu 200000 0"));
        clientPair.appClient.verifyResult(appSync(2, "1 vu 200000 0"));
    }
}