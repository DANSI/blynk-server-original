package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.CreateDevice;
import cc.blynk.server.core.protocol.model.messages.common.HardwareConnectedMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.enums.Priority;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
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
public class DeviceWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();

        this.clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testSendHardwareCommandToNonExistingDevices() throws Exception {
        clientPair.appClient.send("hardware 1-1 vw 100 100");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));
    }

    @Test
    public void testSendHardwareCommandToMultipleDevices() throws Exception {
        Device device0 = new Device(0, "My Dashboard", "UNO");
        Device device1 = new Device(1, "My Device", "ESP8266");

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        assertEqualDevice(device0, devices[0]);
        assertEqualDevice(device1, devices[1]);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("hardware 1 vw 100 100");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("vw 100 100"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(2, b("vw 1 100"))));

        clientPair.appClient.send("hardware 1-0 vw 100 101");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(3, b("vw 100 101"))));
        verify(hardClient2.responseMock, never()).channelRead(any(), eq(new HardwareMessage(3, b("vw 1 101"))));

        clientPair.appClient.send("hardware 1-1 vw 100 102");
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(4, b("vw 100 102"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(new HardwareMessage(4, b("vw 100 102"))));
    }

    @Test
    public void testDeviceWentOfflineMessage() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + device.token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        hardClient2.stop().await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your My Device went offline. \"My Dashboard\" project is disconnected.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testSendHardwareCommandToAppFromMultipleDevices() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(1, device.toString())));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody();

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareConnectedMessage(1, "1-1")));

        clientPair.hardwareClient.send("hardware vw 100 101");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("1 vw 100 101"))));

        hardClient2.send("hardware vw 100 100");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("1-1 vw 100 100"))));
    }

    private static void assertEqualDevice(Device expected, Device real) {
        assertEquals(expected.id, real.id);
        assertEquals(expected.name, real.name);
        assertEquals(expected.boardType, real.boardType);
        assertNotNull(real.token);
    }

}
