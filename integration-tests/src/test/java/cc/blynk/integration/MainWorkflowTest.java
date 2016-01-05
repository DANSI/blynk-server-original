package cc.blynk.integration;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.ResponseWithBodyMessage;
import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.appllication.GetTokenMessage;
import cc.blynk.common.model.messages.protocol.appllication.LoadProfileGzippedBinaryMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.TestAppClient;
import cc.blynk.integration.model.TestHardClient;
import cc.blynk.server.core.application.AppServer;
import cc.blynk.server.core.hardware.HardwareServer;
import cc.blynk.server.handlers.app.main.logic.reporting.GraphPinRequestData;
import cc.blynk.server.reporting.GraphPinRequest;
import cc.blynk.utils.ByteUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static cc.blynk.common.enums.Command.*;
import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MainWorkflowTest extends IntegrationBase {

    private AppServer appServer;
    private HardwareServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        initServerStructures();

        FileUtils.deleteDirectory(holder.fileManager.getDataDir().toFile());

        hardwareServer = new HardwareServer(holder);
        appServer = new AppServer(holder);
        hardwareServer.start();
        appServer.start();

        //todo improve this
        //wait util server starts.
        sleep(500);

        clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        appServer.stop();
        hardwareServer.stop();
        clientPair.stop();
    }

    @Test
    public void testConnectAppAndHardware() throws Exception {
        // we just test that app and hardware can actually connect
    }

    @Test
    public void testHardwareDeviceWentOffline() throws Exception {
        String newProfile = readTestUserProfile("user_profile_json_3_dashes.txt");
        clientPair.appClient.send("saveProfile " + newProfile);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseWithBodyMessage(0, Command.RESPONSE, DEVICE_WENT_OFFLINE_2, 1)));
    }

    @Test
    public void testPingCommandWorks() throws Exception {
        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
    }

    //todo more tests for that case
    @Test
    public void testHardSyncReturnHardwarCommands() throws Exception {
        clientPair.hardwareClient.send("hardsync");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(4)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, "dw 1 1".replaceAll(" ", "\0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, "dw 2 1".replaceAll(" ", "\0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, "aw 3 0".replaceAll(" ", "\0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, "vw 4 244".replaceAll(" ", "\0"))));
    }

    @Test
    public void testHardSyncReturn1HardwarCommand() throws Exception {
        clientPair.hardwareClient.send("hardsync " + "vr 1".replaceAll(" ", "\0"));
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(1)).channelRead(any(), any());

        //todo think how to finish this.
        //verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, "vw 1 1".replaceAll(" ", "\0"))));
    }

    @Test
    public void testPingCommandOk() throws Exception {
        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
    }

    @Test
    public void testDashCommands() throws Exception {
        clientPair.appClient.send("saveDash {\"id\":10, \"name\":\"test board update\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, OK)));

        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, NOT_ALLOWED)));

        clientPair.appClient.send("saveDash {\"id\":10, \"name\":\"test board update\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, OK)));

        clientPair.hardwareClient.send("ping");

        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, OK)));

        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, ILLEGAL_COMMAND)));

        clientPair.appClient.send("loadProfile");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7, LOAD_PROFILE, "{\"dashBoards\":[{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}]}")));

        clientPair.appClient.send("loadProfile 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(8, LOAD_PROFILE, "{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}")));

        clientPair.appClient.send("loadProfile 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(9, ILLEGAL_COMMAND)));

        clientPair.appClient.send("activate 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(10, DEVICE_NOT_IN_NETWORK)));

        clientPair.appClient.send("loadProfile");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(11, LOAD_PROFILE, "{\"dashBoards\":[{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"keepScreenOn\":false,\"isShared\":false,\"isActive\":true}]}")));

        clientPair.appClient.send("saveDash {\"id\":10,\"name\":\"test board update\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(12, OK)));

        clientPair.appClient.send("loadProfile");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(11, LOAD_PROFILE, "{\"dashBoards\":[{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"keepScreenOn\":false,\"isShared\":false,\"isActive\":true}]}")));
    }

    @Test
    public void loadGzippedProfile() throws Exception{
        clientPair.appClient.send("loadprofile");

        String profileString = getBody(clientPair.appClient.responseMock);

        byte[] data = ByteUtils.compress(profileString, 2);
        clientPair.appClient.send("loadProfileGzipped");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new LoadProfileGzippedBinaryMessage(2, data)));

        assertTrue(profileString.length() > data.length);
        System.out.println("Compression ratio : " + ((double) profileString.length() / data.length));
    }

    @Test
    public void testGetGraphEmptyData() throws Exception {
        clientPair.appClient.send("getgraphdata 1 d 8 24 h");

        GraphPinRequest[] array = new GraphPinRequest[] {
                new GraphPinRequestData(1, "d 8 24 h".split(" "), 0, 1, 4)
        };

        //todo find how to check arrays
        verify(blockingIOProcessor, timeout(1000)).readGraphData(any(Channel.class), eq("dima@mail.ua"), any(GraphPinRequest[].class), eq(1));
        //verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, NO_DATA_EXCEPTION)));
    }

    @Test
    public void testDeleteGraphCommandWorks() throws Exception {
        clientPair.appClient.send("getgraphdata 1 d 8 del");

        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
    }

    @Test
    public void testSendEmail() throws Exception {
        blockingIOProcessor.tokenBody = "Auth Token for %s project";
        ClientPair clientPair = initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", null, properties);
        clientPair.appClient.send("email 1");
        verify(blockingIOProcessor, timeout(1000)).mail(any(), eq("dima@mail.ua"), eq("Auth Token for My Dashboard project"), startsWith("Auth Token for My Dashboard project"), eq(1));
    }

    @Test
    public void testAppSendAnyHardCommandAndBack() throws Exception {
        clientPair.appClient.send("hardware 1 dw 1 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "dw 1 1".replaceAll(" ", "\0"))));

        clientPair.hardwareClient.send("hardware ar 1");

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(6, hardMessage.length);
        assertEquals("1 ar 1".replaceAll(" ", "\0"), hardMessage.body);
    }

    @Test
    public void testAppNoActiveDashForHard() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "1 aw 1 1".replaceAll(" ", "\0"))));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(produce(2, NO_ACTIVE_DASHBOARD)));
    }

    @Test
    public void testAppChangeActiveDash() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "1 aw 1 1".replaceAll(" ", "\0"))));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        String newProfile = readTestUserProfile("user_profile_json_3_dashes.txt");
        clientPair.appClient.send("saveProfile " + newProfile);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(produce(2, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(produce(3, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, HARDWARE, "1 aw 1 1".replaceAll(" ", "\0"))));
    }

    @Test
    public void testHardwareLoginWithInfo() throws Exception {
        TestHardClient hardClient2 = new TestHardClient(host, hardPort);
        hardClient2.start(null);

        clientPair.appClient.send("getToken 1");
        String token2 = getBody(clientPair.appClient.responseMock);
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        hardClient2.send("info " + " ver 0.3.1 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100"
                .replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING));

        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, OK)));
    }

    @Test
    public void testActive2AndDeactivate1() throws Exception {
        TestHardClient hardClient2 = new TestHardClient(host, hardPort);
        hardClient2.start(null);

        String newProfile = readTestUserProfile("user_profile_json_3_dashes.txt");
        clientPair.appClient.send("saveProfile " + newProfile);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, OK)));

        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getToken 2");
        String token2 = getBody(clientPair.appClient.responseMock);
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.appClient.reset();

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "1 aw 1 1".replaceAll(" ", "\0"))));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, "2 aw 1 1".replaceAll(" ", "\0"))));


        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(produce(2, NO_ACTIVE_DASHBOARD)));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, "2 aw 1 1".replaceAll(" ", "\0"))));
    }

    @Test
    public void testPushWhenHardwareOffline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(blockingIOProcessor, timeout(500)).push(any(), any(), eq("Your UNO went offline. \"My Dashboard\" project is disconnected."), eq(1));
    }

    @Test
    public void testPushHandler() throws Exception {
        clientPair.hardwareClient.send("push Yo!");

        verify(blockingIOProcessor, timeout(500)).push(any(), any(), eq("Yo!"), eq(1), eq(1));
    }

    @Test
    public void testNoReadingWidgetOnPin() throws Exception {
        clientPair.appClient.send("hardware 1 ar 111");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, ILLEGAL_COMMAND_BODY)));
    }

    @Test
    public void testAppSendWriteHardCommandNotGraphAndBack() throws Exception {
        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "ar 7".replaceAll(" ", "\0"))));

        String body = "aw 7 255";
        clientPair.hardwareClient.send("hardware " + body);

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(("1 " + body).length(), hardMessage.length);
        assertTrue(hardMessage.body.startsWith(("1 " + body).replaceAll(" ", "\0")));
    }


    @Test
    public void testActivateWorkflow() throws Exception {
        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("deactivate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, ILLEGAL_COMMAND)));

        clientPair.appClient.send("hardware 1 ar 1 1");
        //todo check no response
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(produce(3, OK)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, OK)));

        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, HARDWARE, "ar 7".replaceAll(" ", "\0"))));

        String userProfileWithGraph = readTestUserProfile();

        clientPair.appClient.send("saveProfile " + userProfileWithGraph);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, OK)));


        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7, HARDWARE, "ar 7".replaceAll(" ", "\0"))));
    }

    @Test
    public void testTweetNotWorks() throws Exception {
        reset(blockingIOProcessor);

        clientPair.hardwareClient.send("tweet");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, NOTIFICATION_INVALID_BODY_EXCEPTION)));

        clientPair.hardwareClient.send("tweet ");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, NOTIFICATION_INVALID_BODY_EXCEPTION)));

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < 141; i++) {
            a.append("a");
        }

        clientPair.hardwareClient.send("tweet " + a);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, NOTIFICATION_INVALID_BODY_EXCEPTION)));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, NOTIFICATION_NOT_AUTHORIZED_EXCEPTION)));
    }

    @Test
    public void testTweetWorks() throws Exception {
        reset(blockingIOProcessor);
        String userProfileWithTwit = readTestUserProfile();
        clientPair.appClient.send("saveProfile " + userProfileWithTwit);

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.hardwareClient.send("tweet yo");
        verify(blockingIOProcessor, timeout(500)).twit(any(), eq("token"), eq("secret"), eq("yo"), eq(1));

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, QUOTA_LIMIT_EXCEPTION)));
    }

    @Test
    public void testWrongCommandForAggregation() throws Exception {
        clientPair.hardwareClient.send("hardware vw 10 aaaa");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "1 vw 10 aaaa".replaceAll(" ", "\0"))));
    }

    @Test
    public void testAppSendWriteHardCommandForGraphAndBack() throws Exception {
        String userProfileWithGraph = readTestUserProfile();
        clientPair.appClient.send("saveProfile " + userProfileWithGraph);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        reset(clientPair.appClient.responseMock);
        clientPair.appClient.reset();

        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "ar 7".replaceAll(" ", "\0"))));

        String body = "aw 8 333";
        clientPair.hardwareClient.send("hardware " + body);

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        //"aw 11 333".length + ts.length + separator
        assertEquals(("1 " + body).length() + 14, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(("1 " + body).replaceAll(" ", "\0")));
    }

    @Test
    //todo resolve it.
    //todo more tests for that
    public void testSendPinModeCommandWhenHardwareGoesOnline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        assertTrue(channelFuture.isDone());

        String body = "pm 13 in";
        clientPair.appClient.send("hardware 1 " + body);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, DEVICE_NOT_IN_NETWORK)));

        TestHardClient hardClient = new TestHardClient(host, hardPort);
        hardClient.start(null);
        hardClient.send("login " + clientPair.token);
        verify(hardClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(1, OK)));
        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, body.replaceAll(" ", "\0"))));
        verify(hardClient.responseMock, times(2)).channelRead(any(), any());
    }

    @Test
    public void testClosedConnectionWhenNotLogged() throws Exception {
        TestAppClient appClient2 = new TestAppClient(host, appPort, properties);
        appClient2.start(null);
        appClient2.send("getToken 1");
        verify(appClient2.responseMock, after(400).never()).channelRead(any(), any());
        assertTrue(appClient2.isClosed());

        appClient2.send("login dima@mail.ua 1 Android 1RC7");
        verify(appClient2.responseMock, after(200).never()).channelRead(any(), any());
    }

    @Test
    public void testSendEmptyPinModeCommandWhenHardwareGoesOnline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        assertTrue(channelFuture.isDone());

        clientPair.appClient.send("hardware 1 pm 13 in");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, DEVICE_NOT_IN_NETWORK)));

        TestHardClient hardClient = new TestHardClient(host, hardPort);
        hardClient.start(null);
        hardClient.send("login " + clientPair.token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));

        //todo ping?
        //verify(hardClient.responseMock, times(1)).channelRead(any(), any());
    }

    @Test
    public void testSendHardwareCommandToNotActiveDashboard() throws Exception {
        clientPair = initAppAndHardPair("user_profile_json_3_dashes.txt");

        clientPair.appClient.send("getToken 2");

        //getting token for second GetTokenMessage
        ArgumentCaptor<GetTokenMessage> objectArgumentCaptor = ArgumentCaptor.forClass(GetTokenMessage.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());
        List<GetTokenMessage> arguments = objectArgumentCaptor.getAllValues();
        GetTokenMessage getTokenMessage = arguments.get(0);
        String token = getTokenMessage.body;

        clientPair.appClient.reset();

        //connecting separate hardware to non active dashboard
        TestHardClient nonActiveDashHardClient = new TestHardClient(host, hardPort);
        nonActiveDashHardClient.start(null);
        nonActiveDashHardClient.send("login " + token);
        verify(nonActiveDashHardClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(1, OK)));
        nonActiveDashHardClient.reset();


        //sending hardware command from hardware that has no active dashboard
        nonActiveDashHardClient.send("hardware aw 1 1");
        verify(nonActiveDashHardClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, NO_ACTIVE_DASHBOARD)));
        verify(clientPair.appClient.responseMock, timeout(1000).times(0)).channelRead(any(), any());

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(0)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, "1 aw 1 1".replaceAll(" ", "\0"))));

    }

    @Test
    public void testConnectAppAndHardwareAndSendCommands() throws Exception {
        for (int i = 0; i < 100; i++) {
            clientPair.appClient.send("hardware 1 aw 1 1");
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500).times(100)).channelRead(any(), any());
    }

    @Test
    public void testSendReadCommandsForDifferentPins() throws Exception {
        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, "ar 7".replaceAll(" ", "\0"))));

        clientPair.hardwareClient.reset();

        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), any());

        clientPair.appClient.send("hardware 1 ar 6");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, "ar 6".replaceAll(" ", "\0"))));

        clientPair.hardwareClient.reset();

        clientPair.appClient.send("hardware 1 ar 6");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), any());

        Thread.sleep(100);
        clientPair.appClient.send("hardware 1 ar 6");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, HARDWARE, "ar 6".replaceAll(" ", "\0"))));
    }

    @Test
    //todo one more test here
    public void test2ClientPairsWorkCorrectly() throws Exception {
        final int ITERATIONS = 100;
        ClientPair clientPair2 = initAppAndHardPair("localhost", appPort, hardPort, "dima2@mail.ua 1", null, properties);

        String body = "ar 7";
        for (int i = 1; i <= ITERATIONS; i++) {
            clientPair.appClient.send("hardware 1 " + body);
            clientPair2.appClient.send("hardware 1 " + body);
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), any());
        verify(clientPair2.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), any());


        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, body.replaceAll(" ", "\0"))));
        verify(clientPair2.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, body.replaceAll(" ", "\0"))));
    }

    @Test
    public void testTryReachQuotaLimit() throws Exception {
        String body = "1 aw 100 100";

        //within 1 second sending more messages than default limit 100.
        for (int i = 0; i < 1000 / 9; i++) {
            clientPair.hardwareClient.send("hardware " + body);
            sleep(9);
        }

        ArgumentCaptor<ResponseMessage> objectArgumentCaptor = ArgumentCaptor.forClass(ResponseMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<ResponseMessage> arguments = objectArgumentCaptor.getAllValues();
        ResponseMessage responseMessage = arguments.get(0);
        assertTrue(responseMessage.id > 100);

        //at least 100 iterations should be
        for (int i = 0; i < 100; i++) {
            verify(clientPair.appClient.responseMock).channelRead(any(), eq(produce(i+1, HARDWARE, ("1 " + body).replaceAll(" ", "\0"))));
        }

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        //check no more accepted
        for (int i = 0; i < 10; i++) {
            clientPair.hardwareClient.send("hardware " + body);
            sleep(9);
        }

        verify(clientPair.hardwareClient.responseMock, times(0)).channelRead(any(), eq(produce(1, QUOTA_LIMIT_EXCEPTION)));
        verify(clientPair.appClient.responseMock, times(0)).channelRead(any(), eq(produce(1, HARDWARE, body.replaceAll(" ", "\0"))));
    }

    @Test
    @Ignore("hard to test this case...")
    public void testTryReachQuotaLimitAndWarningExceededLimit() throws Exception {
        String body = "1 ar 100 100";

        //within 1 second sending more messages than default limit 100.
        for (int i = 0; i < 1000 / 9; i++) {
            clientPair.appClient.send("hardware " + body, 1);
            sleep(9);
        }

        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, QUOTA_LIMIT_EXCEPTION)));
        verify(clientPair.hardwareClient.responseMock, atLeast(100)).channelRead(any(), eq(produce(1, HARDWARE, body.replaceAll(" ", "\0"))));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        //waiting to avoid limit.
        sleep(1000);

        for (int i = 0; i < 100000 / 9; i++) {
            clientPair.appClient.send("hardware " + body, 1);
            sleep(9);
        }

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, QUOTA_LIMIT_EXCEPTION)));
        verify(clientPair.hardwareClient.responseMock, atLeast(100)).channelRead(any(), eq(produce(1, HARDWARE, body.replaceAll(" ", "\0"))));

    }

}
