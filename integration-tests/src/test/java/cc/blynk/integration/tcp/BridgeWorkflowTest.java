package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.api.http.AppAndHttpsServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.TokenGeneratorUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertNotNull;
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
public class BridgeWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;
    private String token = TokenGeneratorUtil.generateNewToken();

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppAndHttpsServer(holder).start();
        this.clientPair = initAppAndHardPair("user_profile_json_3_dashes.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testBridgeInitOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testBridgeInitIllegalCommand() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i");
        clientPair.hardwareClient.verifyResult(illegalCommand(1));

        clientPair.hardwareClient.send("bridge i");
        clientPair.hardwareClient.verifyResult(illegalCommand(2));

        clientPair.hardwareClient.send("bridge 1 auth_tone");
        clientPair.hardwareClient.verifyResult(illegalCommand(3));

        clientPair.hardwareClient.send("bridge 1");
        clientPair.hardwareClient.verifyResult(illegalCommand(4));

        clientPair.hardwareClient.send("bridge 1");
        clientPair.hardwareClient.verifyResult(illegalCommand(5));
    }

    @Test
    public void testSeveralBridgeInitOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        clientPair.hardwareClient.send("bridge 2 i " + clientPair.token);
        clientPair.hardwareClient.send("bridge 3 i " + clientPair.token);
        clientPair.hardwareClient.send("bridge 4 i " + clientPair.token);
        clientPair.hardwareClient.verifyResult(ok(1));
        clientPair.hardwareClient.verifyResult(ok(2));
        clientPair.hardwareClient.verifyResult(ok(3));
        clientPair.hardwareClient.verifyResult(ok(4));

        clientPair.hardwareClient.send("bridge 5 i " + clientPair.token);
        clientPair.hardwareClient.send("bridge 5 i " + clientPair.token);
        clientPair.hardwareClient.send("bridge 5 i " + clientPair.token);
        clientPair.hardwareClient.send("bridge 5 i " + clientPair.token);
        clientPair.hardwareClient.verifyResult(ok(5));
        clientPair.hardwareClient.verifyResult(ok(6));
        clientPair.hardwareClient.verifyResult(ok(7));
        clientPair.hardwareClient.verifyResult(ok(8));
    }

    @Test
    public void testBridgeInitAndOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testBridgeWithoutInit() throws Exception {
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        clientPair.hardwareClient.verifyResult(notAllowed(1));
    }

    @Test
    public void testBridgeInitAndSendNoOtherDevices() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        clientPair.hardwareClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        clientPair.hardwareClient.verifyResult(new ResponseMessage(2, DEVICE_NOT_IN_NETWORK));
    }

    @Test
    public void testBridgeInitAndSendOtherDevicesButNoBridgeDevices() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.reset();

        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.login(clientPair.token);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + device.token);
        clientPair.hardwareClient.verifyResult(ok(1));
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        clientPair.hardwareClient.verifyResult(new ResponseMessage(2, DEVICE_NOT_IN_NETWORK));
    }

    @Test
    public void testSecondTokenNotInitialized() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        clientPair.hardwareClient.verifyResult(ok(1));
        clientPair.hardwareClient.send("bridge 2 aw 10 10");
        clientPair.hardwareClient.verifyResult(notAllowed(2));
    }

    @Test
    public void testCorrectWorkflow2HardsSameToken() throws Exception {
        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.login(clientPair.token);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        clientPair.hardwareClient.verifyResult(ok(1));
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        hardClient1.verifyResult(bridge(2, "aw 10 10"));
        clientPair.appClient.verifyResult(hardware(2, "1-0 aw 10 10"));
    }

    @Test
    public void testCorrectWorkflow2HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;

        clientPair.appClient.activate(2);
        clientPair.appClient.verifyResult(new ResponseMessage(2, DEVICE_NOT_IN_NETWORK));
        clientPair.appClient.reset();

        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.login(token2);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + token2);
        clientPair.hardwareClient.verifyResult(ok(1));
        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        hardClient1.verifyResult(bridge(2, "aw 11 11"));
        clientPair.appClient.verifyResult(hardwareConnected(1, "2-0"));
        clientPair.appClient.verifyResult(hardware(2, "2-0 aw 11 11"));
    }

    @Test
    public void testCorrectWorkflow3HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        clientPair.appClient.reset();

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.login(token2);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(token2);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        clientPair.hardwareClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        hardClient1.verifyResult(bridge(2, "aw 11 11"));
        hardClient2.verifyResult(bridge(2, "aw 11 11"));

        clientPair.appClient.verifyResult(hardwareConnected(1, "2-0"), 2);
        clientPair.appClient.never(hardware(2, "2 aw 11 11"));
    }

    @Test
    public void testCorrectWorkflow4HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");
        clientPair.appClient.send("getToken 3");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(2)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        String token3 = ((StringMessage) arguments.get(1)).body;

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.login(token2);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(token2);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();

        TestHardClient hardClient3 = new TestHardClient("localhost", tcpHardPort);
        hardClient3.start();
        hardClient3.login(token3);
        verify(hardClient3.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
        hardClient3.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        clientPair.hardwareClient.verifyResult(ok(1));
        clientPair.hardwareClient.send("bridge 2 i " + token3);
        clientPair.hardwareClient.verifyResult(ok(1));


        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        hardClient1.verifyResult(bridge(3, "aw 11 11"));
        hardClient2.verifyResult(bridge(3, "aw 11 11"));

        clientPair.hardwareClient.send("bridge 2 aw 13 13");
        hardClient3.verifyResult(bridge(4, "aw 13 13"));
    }

    @Test
    public void testCorrectWorkflow3HardsDifferentTokenAndSync() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        clientPair.appClient.reset();

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.login(token2);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(token2);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        clientPair.hardwareClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        hardClient1.verifyResult(bridge(2, "aw 11 11"));
        hardClient2.verifyResult(bridge(2, "aw 11 11"));

        clientPair.appClient.verifyResult(produce(1, HARDWARE_CONNECTED, "2-0"), 2);
        clientPair.appClient.never(hardware(2, "2 aw 11 11"));

        hardClient1.sync(PinType.ANALOG, 11);
        hardClient1.verifyResult(hardware(1, "aw 11 11"));
        hardClient2.sync(PinType.ANALOG, 11);
        hardClient2.verifyResult(hardware(1, "aw 11 11"));
    }

    @Test
    public void testCorrectWorkflow4HardsDifferentTokenAndSync() throws Exception {
        clientPair.appClient.send("getToken 2");
        clientPair.appClient.send("getToken 3");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(2)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((StringMessage) arguments.get(0)).body;
        String token3 = ((StringMessage) arguments.get(1)).body;

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient("localhost", tcpHardPort);
        hardClient1.start();
        hardClient1.login(token2);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(token2);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();

        TestHardClient hardClient3 = new TestHardClient("localhost", tcpHardPort);
        hardClient3.start();
        hardClient3.login(token3);
        verify(hardClient3.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
        hardClient3.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        clientPair.hardwareClient.verifyResult(ok(1));
        clientPair.hardwareClient.send("bridge 2 i " + token3);
        clientPair.hardwareClient.verifyResult(ok(1));


        clientPair.hardwareClient.send("bridge 1 vw 11 12");
        hardClient1.verifyResult(bridge(3, "vw 11 12"));
        hardClient2.verifyResult(bridge(3, "vw 11 12"));

        clientPair.hardwareClient.send("bridge 2 aw 13 13");
        hardClient3.verifyResult(bridge(4, "aw 13 13"));

        hardClient1.sync(PinType.VIRTUAL, 11);
        hardClient1.verifyResult(hardware(1, "vw 11 12"));
        hardClient2.sync(PinType.VIRTUAL, 11);
        hardClient2.verifyResult(hardware(1, "vw 11 12"));
        hardClient3.sync(PinType.ANALOG, 13);
        hardClient3.verifyResult(hardware(1, "aw 13 13"));
        hardClient3.sync(PinType.ANALOG, 13);
        hardClient3.never(hardware(2, "aw 13 13"));
    }

    @Test
    public void bridgeOnlyWorksWithinOneAccount() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.register("test@test.com", "1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient.login("test@test.com", "1", "Android", "RC13");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        appClient.createDash("{\"id\":1, \"createdAt\":1, \"name\":\"test board\"}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        appClient.reset();

        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        appClient.createDevice(1, device1);
        String createdDevice = appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice, 0);
        assertNotNull(device);
        assertNotNull(device.token);
        appClient.verifyResult(createDevice(1, device));

        appClient.reset();

        clientPair.hardwareClient.send("bridge 1 i " + device.token);
        clientPair.hardwareClient.verifyResult(notAllowed(1));
    }

}
