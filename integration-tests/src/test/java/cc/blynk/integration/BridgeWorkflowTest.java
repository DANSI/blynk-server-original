package cc.blynk.integration;

import cc.blynk.common.model.messages.Message;
import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.TestHardClient;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.application.AppServer;
import cc.blynk.server.core.hardware.HardwareServer;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static cc.blynk.common.enums.Command.BRIDGE;
import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class BridgeWorkflowTest extends IntegrationBase {

    private AppServer appServer;
    private HardwareServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        initServerStructures();

        FileUtils.deleteDirectory(fileManager.getDataDir().toFile());

        hardwareServer = new HardwareServer(properties, userRegistry, sessionsHolder, stats, notificationsProcessor, new TransportTypeHolder(properties), storage);
        appServer = new AppServer(properties, userRegistry, sessionsHolder, stats, new TransportTypeHolder(properties), storage);
        new Thread(hardwareServer).start();
        new Thread(appServer).start();

        //todo improve this
        //wait util server starts.
        sleep(500);

        clientPair = initAppAndHardPair("user_profile_json_3_dashes.txt");
    }

    @After
    public void shutdown() {
        appServer.stop();
        hardwareServer.stop();
        clientPair.stop();
    }

    @Test
    public void testBridgeInitOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i auth_token");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
    }

    @Test
    public void testBridgeInitIllegalCommand() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge i");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge 1 auth_tone");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("bridge 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, ILLEGAL_COMMAND)));
    }

    @Test
    public void testSeveralBridgeInitOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i auth_token");
        clientPair.hardwareClient.send("bridge 2 i auth_token");
        clientPair.hardwareClient.send("bridge 3 i auth_token");
        clientPair.hardwareClient.send("bridge 4 i auth_token");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, OK)));

        clientPair.hardwareClient.send("bridge 5 i auth_token");
        clientPair.hardwareClient.send("bridge 5 i auth_token");
        clientPair.hardwareClient.send("bridge 5 i auth_token");
        clientPair.hardwareClient.send("bridge 5 i auth_token");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7, OK)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(8, OK)));
    }

    @Test
    public void testBridgeInitAndOk() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i auth_token");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
    }

    @Test
    public void testBridgeWithoutInit() throws Exception {
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, NOT_ALLOWED)));
    }

    @Test
    public void testBridgeInitAndSendNoOtherDevices() throws Exception {
        clientPair.hardwareClient.send("bridge 1 i auth_token");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        //no OK, cause nothings send back
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEVICE_NOT_IN_NETWORK)));
    }

    @Test
    public void testCorrectWorkflow2HardsSameToken() throws Exception {
        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient(host, hardPort);
        hardClient1.start(null);
        hardClient1.send("login " + clientPair.token);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + clientPair.token);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
        clientPair.hardwareClient.send("bridge 1 aw 10 10");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, "aw 10 10".replaceAll(" ", "\0"))));
    }

    @Test
    public void testCorrectWorkflow2HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((Message) arguments.get(0)).body;

        //creating 1 new hard client
        TestHardClient hardClient1 = new TestHardClient(host, hardPort);
        hardClient1.start(null);
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
        hardClient1.reset();

        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, "aw 11 11".replaceAll(" ", "\0"))));
    }

    @Test
    public void testCorrectWorkflow3HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((Message) arguments.get(0)).body;

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient(host, hardPort);
        hardClient1.start(null);
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient(host, hardPort);
        hardClient2.start(null);
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
        hardClient2.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, "aw 11 11".replaceAll(" ", "\0"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, BRIDGE, "aw 11 11".replaceAll(" ", "\0"))));

    }

    @Test
    public void testCorrectWorkflow4HardsDifferentToken() throws Exception {
        clientPair.appClient.send("getToken 2");
        clientPair.appClient.send("getToken 3");

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(2)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token2 = ((Message) arguments.get(0)).body;
        String token3 = ((Message) arguments.get(1)).body;

        //creating 2 new hard clients
        TestHardClient hardClient1 = new TestHardClient(host, hardPort);
        hardClient1.start(null);
        hardClient1.send("login " + token2);
        verify(hardClient1.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient(host, hardPort);
        hardClient2.start(null);
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
        hardClient2.reset();

        TestHardClient hardClient3 = new TestHardClient(host, hardPort);
        hardClient3.start(null);
        hardClient3.send("login " + token3);
        verify(hardClient3.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
        hardClient3.reset();


        clientPair.hardwareClient.send("bridge 1 i " + token2);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
        clientPair.hardwareClient.send("bridge 2 i " + token3);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));


        clientPair.hardwareClient.send("bridge 1 aw 11 11");
        verify(hardClient1.responseMock, timeout(500)).channelRead(any(), eq(produce(3, BRIDGE, "aw 11 11".replaceAll(" ", "\0"))));
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(3, BRIDGE, "aw 11 11".replaceAll(" ", "\0"))));

        clientPair.hardwareClient.send("bridge 2 aw 13 13");
        verify(hardClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(4, BRIDGE, "aw 13 13".replaceAll(" ", "\0"))));

    }


}
