package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.BinaryMessage;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.ResponseWithBodyMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetTokenMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoadProfileGzippedBinaryMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.SyncMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareConnectedMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.enums.Priority;
import cc.blynk.server.workers.StorageWorker;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ByteUtils;
import cc.blynk.utils.JsonParser;
import io.netty.channel.ChannelFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.*;
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

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
        this.appServer = new AppServer(holder).start(transportTypeHolder);

        this.clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void createBasicProfile() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.send("login test@test.com 1 Android RC13");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        appClient.send("createDash {\"id\":1, \"createdAt\":1, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        appClient.send("createWidget 1\0{\"id\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":1}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(appClient.getBody(), 1);
        profile.dashBoards[0].updatedAt = 0;

        assertEquals("{\"dashBoards\":[{\"id\":1,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"label\":\"Some Text\",\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false,\"invertedOn\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());

        appClient.send("createWidget 1\0{\"id\":2, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = JsonParser.parseProfile(appClient.getBody(), 1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"label\":\"Some Text\",\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false,\"invertedOn\":false},{\"type\":\"BUTTON\",\"id\":2,\"x\":2,\"y\":2,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"label\":\"Some Text 2\",\"pinType\":\"DIGITAL\",\"pin\":2,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false,\"invertedOn\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());

        appClient.send("updateWidget 1\0{\"id\":2, \"x\":2, \"y\":2, \"label\":\"new label\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":3}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = JsonParser.parseProfile(appClient.getBody(), 1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"label\":\"Some Text\",\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false,\"invertedOn\":false},{\"type\":\"BUTTON\",\"id\":2,\"x\":2,\"y\":2,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"label\":\"new label\",\"pinType\":\"DIGITAL\",\"pin\":3,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false,\"invertedOn\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());

        appClient.send("deleteWidget 1\0" + "3");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));

        appClient.send("deleteWidget 1\0" + "1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        appClient.send("deleteWidget 1\0" + "2");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        profile = JsonParser.parseProfile(appClient.getBody(), 1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals("{\"dashBoards\":[{\"id\":1,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());
    }

    @Test
    public void testConnectAppAndHardware() throws Exception {
        // we just test that app and hardware can actually connect
    }

    @Test
    public void testRegisterWithAnotherApp() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.send("login test@test.com 1 Android 1.13.3 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        appClient.send("createDash {\"id\":1, \"createdAt\":1, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        appClient.send("createWidget 1\0{\"id\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":1}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        appClient.reset();

        appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(appClient.getBody(), 1);
        profile.dashBoards[0].updatedAt = 0;

        assertEquals("{\"dashBoards\":[{\"id\":1,\"name\":\"test board\",\"createdAt\":1,\"updatedAt\":0,\"widgets\":[{\"type\":\"BUTTON\",\"id\":1,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"label\":\"Some Text\",\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"pushMode\":false,\"invertedOn\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}]}", profile.toString());
    }

    @Test
    public void testNoEnergyDrainForBusinessApps() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);

        appClient.start();

        appClient.send("register test@test.com 1 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.send("login test@test.com 1 Android 1.13.3 MyApp");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        appClient.send("createDash {\"id\":2, \"createdAt\":1458856800001, \"name\":\"test board\"}\"");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        appClient.send("getEnergy");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, GET_ENERGY, "2000")));

        appClient.send("createWidget 2\0{\"id\":2, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, OK)));

        appClient.send("createWidget 2\0{\"id\":3, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(6, OK)));

        appClient.send("createWidget 2\0{\"id\":4, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(7, OK)));

        appClient.send("createWidget 2\0{\"id\":5, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(8, OK)));

        appClient.send("createWidget 2\0{\"id\":6, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"LCD\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(9, OK)));

        appClient.send("createWidget 2\0{\"id\":7, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(10, OK)));
    }

    @Test
    public void testHardwareDeviceWentOffline() throws Exception {
        Profile profile = JsonParser.parseProfile(readTestUserProfile(), 1);
        Notification notification = profile.getDashById(1).getWidgetByType(Notification.class);
        notification.notifyWhenOffline = false;

        clientPair.appClient.send("saveDash " + profile.getDashById(1).toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseWithBodyMessage(0, Command.RESPONSE, DEVICE_WENT_OFFLINE, 1)));
    }

    @Test
    public void testPingCommandWorks() throws Exception {
        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
    }

    //todo more tests for that case
    @Test
    public void testHardSyncReturnHardwareCommands() throws Exception {
        clientPair.hardwareClient.send("hardsync");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(6)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
    }

    @Test
    public void testHardSyncReturnNothingNoWidgetOnPin() throws Exception {
        clientPair.hardwareClient.send("hardsync " + b("vr 22"));
        verify(clientPair.hardwareClient.responseMock, after(400).never()).channelRead(any(), any());
    }

    @Test
    public void testHardSyncReturn1HardwareCommand() throws Exception {
        clientPair.hardwareClient.send("hardsync " + b("vr 4"));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
    }

    @Test
    public void testHardSyncReturnRTCWithoutTimezone() throws Exception {
        clientPair.hardwareClient.send("hardsync " + b("vr 9"));

        long expectedTS = System.currentTimeMillis() / 1000;

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(15, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 9")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezone() throws Exception {
        ZoneOffset offset = ZoneOffset.of("+00:00");

        clientPair.appClient.send(("createWidget 1\0{\"type\":\"RTC\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":0,\"height\":0," +
                "\"timezone\":\"TZ\"}").replace("TZ", offset.toString()));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardsync " + b("vr 99"));

        long expectedTS = System.currentTimeMillis() / 1000 + offset.getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(16, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 99")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezonePlus3() throws Exception {
        ZoneOffset offset = ZoneOffset.of("+03:00");

        clientPair.appClient.send(("createWidget 1\0{\"type\":\"RTC\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":0,\"height\":0," +
                "\"timezone\":\"TZ\"}").replace("TZ", offset.toString()));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardsync " + b("vr 99"));

        long expectedTS = System.currentTimeMillis() / 1000 + offset.getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(16, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 99")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezoneMinus3() throws Exception {
        ZoneOffset offset = ZoneOffset.of("-03:00");

        clientPair.appClient.send(("createWidget 1\0{\"type\":\"RTC\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":0,\"height\":0," +
                "\"timezone\":\"TZ\"}").replace("TZ", offset.toString()));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardsync " + b("vr 99"));

        long expectedTS = System.currentTimeMillis() / 1000 + offset.getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(16, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 99")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testAddAndRemoveTabs() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        assertEquals(13, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "9200")));

        clientPair.appClient.send("createWidget 1\0{\"id\":100, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}, {\"label\":\"tab 3\"}], \"type\":\"TABS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.send("createWidget 1\0{\"id\":101, \"x\":15, \"y\":0, \"tabId\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":18}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, OK)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, GET_ENERGY, "8800")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("deleteWidget 1\0" + "100");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, GET_ENERGY, "9000")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        assertEquals(14, profile.dashBoards[0].widgets.length);
        assertNotNull(profile.dashBoards[0].findWidgetByPin((byte) 17, PinType.DIGITAL));
    }

    @Test
    public void testAddAndUpdateTabs() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        assertEquals(13, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "9200")));

        clientPair.appClient.send("createWidget 1\0{\"id\":100, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}, {\"label\":\"tab 3\"}], \"type\":\"TABS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.send("createWidget 1\0{\"id\":101, \"x\":15, \"y\":0, \"tabId\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":18}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"x\":5, \"y\":0, \"tabId\":2, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, OK)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, GET_ENERGY, "8800")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("updateWidget 1\0{\"id\":100, \"x\":0, \"y\":0, \"tabs\":[{\"label\":\"tab 1\"}, {\"label\":\"tab 2\"}], \"type\":\"TABS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, GET_ENERGY, "9000")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        assertEquals(15, profile.dashBoards[0].widgets.length);
        assertNull(profile.dashBoards[0].findWidgetByPin((byte) 17, PinType.DIGITAL));
        assertNotNull(profile.dashBoards[0].findWidgetByPin((byte) 18, PinType.DIGITAL));
    }

    @Test
    public void testPurchaseEnergy() throws Exception {
        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "5262996016779471529.4493624392154338");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "A3B93EE9-BC65-499E-A660-F2A84F2AF1FC");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "com.blynk.energy.280001461578468247");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, NOT_ALLOWED)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "150000195113772");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(6, OK)));

        clientPair.appClient.send("addEnergy " + "1000" + "\0" + "1370-3990-1414-55681");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(7, OK)));
    }

    @Test
    public void testApplicationPingCommandOk() throws Exception {
        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("ping");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
    }

    @Test
    public void testHardPingCommandOk() throws Exception {
        clientPair.hardwareClient.send("ping");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("ping");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
    }

    @Test
    public void testDashCommands() throws Exception {
        clientPair.appClient.send("saveDash {\"id\":10, \"name\":\"test board update\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NOT_ALLOWED)));

        clientPair.appClient.send("saveDash {\"id\":10, \"name\":\"test board update\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.hardwareClient.send("ping");

        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, OK)));

        clientPair.appClient.send("deleteDash 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(6, ILLEGAL_COMMAND)));

        String expectedDash;

        expectedDash = "{\"dashBoards\":[{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}]}";
        clientPair.appClient.send("loadProfileGzipped");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new LoadProfileGzippedBinaryMessage(7, ByteUtils.compress(expectedDash))));

        expectedDash = "{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}";
        clientPair.appClient.send("loadProfileGzipped 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new LoadProfileGzippedBinaryMessage(8, ByteUtils.compress(expectedDash))));

        clientPair.appClient.send("loadProfileGzipped 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(9, ILLEGAL_COMMAND)));

        clientPair.appClient.send("activate 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(10, DEVICE_NOT_IN_NETWORK)));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        expectedDash = String.format("{\"dashBoards\":[{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":%d,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":true}]}", profile.dashBoards[0].updatedAt);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new LoadProfileGzippedBinaryMessage(1, ByteUtils.compress(expectedDash))));

        clientPair.appClient.send("saveDash {\"id\":10,\"name\":\"test board update\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        expectedDash = "{\"dashBoards\":[{\"id\":10,\"name\":\"test board update\",\"createdAt\":0,\"updatedAt\":0,\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isShared\":false,\"isActive\":true}]}";
        clientPair.appClient.send("loadProfileGzipped");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new LoadProfileGzippedBinaryMessage(3, ByteUtils.compress(expectedDash))));
    }

    @Test
    public void deleteDashDeletesTokensAlso() throws Exception {
        clientPair.appClient.send("createDash {\"id\":10, \"name\":\"test board\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getToken 10");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);

        clientPair.appClient.reset();
        clientPair.appClient.send("getShareToken 10");
        String sharedToken = clientPair.appClient.getBody();
        assertNotNull(sharedToken);

        clientPair.appClient.send("deleteDash 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        //todo on delete also close existing connections?
        TestHardClient newHardClient = new TestHardClient("localhost", tcpHardPort);
        newHardClient.start();
        newHardClient.send("login " + token);
        verify(newHardClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, INVALID_TOKEN)));

        TestAppClient newAppClient = new TestAppClient("localhost", tcpAppPort, properties);
        newAppClient.start();
        newAppClient.send("shareLogin " + "dima@mail.ua " + sharedToken + " Android 24");

        verify(newAppClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));
    }

    @Test
    public void loadGzippedProfile() throws Exception{
        String expected = readTestUserProfile();

        clientPair.appClient.send("loadProfileGzipped");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), any());

        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody(), 1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals(expected, profile.toString());
    }

    @Test
    public void testGetGraphDataFor1Pin() throws Exception {
        String tempDir = holder.props.getProperty("data.folder");

        final Path userReportFolder = Paths.get(tempDir, "data", DEFAULT_TEST_USER);
        if (Files.notExists(userReportFolder)) {
            Files.createDirectories(userReportFolder);
        }

        Path pinReportingDataPath = Paths.get(tempDir, "data", DEFAULT_TEST_USER, ReportingDao.generateFilename(1, PinType.DIGITAL, (byte) 8, GraphType.HOURLY));

        StorageWorker.write(pinReportingDataPath, 1.11D, 1111111);
        StorageWorker.write(pinReportingDataPath, 1.22D, 2222222);

        clientPair.appClient.send("getgraphdata 1 d 8 24 h");

        ArgumentCaptor<BinaryMessage> objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        BinaryMessage graphDataResponse = objectArgumentCaptor.getValue();

        assertNotNull(graphDataResponse);
        byte[] decompressedGraphData = ByteUtils.decompress(graphDataResponse.getBytes());
        ByteBuffer bb = ByteBuffer.wrap(decompressedGraphData);

        assertEquals(1, bb.getInt());
        assertEquals(2, bb.getInt());
        assertEquals(1.11D, bb.getDouble(), 0.1);
        assertEquals(1111111, bb.getLong());
        assertEquals(1.22D, bb.getDouble(), 0.1);
        assertEquals(2222222, bb.getLong());

    }

    @Test
    public void testDeleteGraphCommandWorks() throws Exception {
        clientPair.appClient.send("getgraphdata 1 d 8 del");

        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
    }

    @Test
    public void testSendEmail() throws Exception {
        blockingIOProcessor.tokenBody = "Auth Token for %s project";

        TestAppClient appClient = new TestAppClient("localhost", tcpAppPort, properties);
        appClient.start();
        appClient.send("login dima@mail.ua 1");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.send("email 1");
        verify(mailWrapper, timeout(1000)).send(eq(DEFAULT_TEST_USER), eq("Auth Token for My Dashboard project"), startsWith("Auth Token for My Dashboard project"));
    }

    @Test
    public void testSendUnicodeChar() throws Exception {
        clientPair.hardwareClient.send("hardware vw 1 °F");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 1 °F"))));
    }

    @Test
    public void testAppSendAnyHardCommandAndBack() throws Exception {
        clientPair.appClient.send("hardware 1 dw 1 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));

        clientPair.hardwareClient.send("hardware ar 1");

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(6, hardMessage.length);
        assertEquals(b("1 ar 1"), hardMessage.body);
    }

    @Test
    public void testAppNoActiveDashForHard() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));
    }

    @Test
    public void testHardwareSendsWrongCommand() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 ");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.hardwareClient.send("hardware aw 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));
    }

    @Test
    public void testAppChangeActiveDash() throws Exception {
        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        Profile newProfile = JsonParser.parseProfile(readTestUserProfile("user_profile_json_3_dashes.txt"), 1);
        clientPair.appClient.send("createDash " + newProfile.dashBoards[1]);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(new ResponseMessage(3, NO_ACTIVE_DASHBOARD)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, HARDWARE, b("1 aw 1 1"))));
    }

    @Test
    public void testHardwareLoginWithInfo() throws Exception {
        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        clientPair.appClient.send("getToken 1");
        String token2 = clientPair.appClient.getBody();
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        hardClient2.send("info " + b(" ver 0.3.1 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100"));

        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        hardClient2.stop().awaitUninterruptibly();
    }

    @Test
    public void testActive2AndDeactivate1() throws Exception {
        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        Profile newProfile = JsonParser.parseProfile(readTestUserProfile("user_profile_json_3_dashes.txt"), 1);
        clientPair.appClient.send("createDash " + newProfile.dashBoards[1]);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, DEVICE_NOT_IN_NETWORK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getToken 2");
        String token2 = clientPair.appClient.getBody();
        hardClient2.send("login " + token2);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("2 aw 1 1"))));


        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500).times(0)).channelRead(any(), eq(new ResponseMessage(2, NO_ACTIVE_DASHBOARD)));

        hardClient2.send("hardware aw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("2 aw 1 1"))));
        hardClient2.stop().awaitUninterruptibly();
    }

    @Test
    public void testPushWhenHardwareOffline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Your UNO went offline. \"My Dashboard\" project is disconnected.", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testPushHandler() throws Exception {
        clientPair.hardwareClient.send("push Yo!");

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(gcmWrapper, timeout(500).times(1)).send(objectArgumentCaptor.capture());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Yo!", 1).toJson();
        assertEquals(expectedJson, message.toJson());
    }

    @Test
    public void testNoReadingWidgetOnPin() throws Exception {
        clientPair.appClient.send("hardware 1 ar 111");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND_BODY)));
    }

    @Test
    public void testAppSendWriteHardCommandNotGraphAndBack() throws Exception {
        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("ar 7"))));

        String body = "aw 7 255";
        clientPair.hardwareClient.send("hardware " + body);

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(("1 " + body).length(), hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("1 " + body)));
    }


    @Test
    public void testActivateWorkflow() throws Exception {
        clientPair.appClient.send("activate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("deactivate 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));

        clientPair.appClient.send("hardware 1 ar 1 1");
        //todo check no response
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, HARDWARE, b("ar 7"))));
    }

    @Test
    public void testActivateAndGetSync() throws Exception {
        clientPair.appClient.send("activate 1");

        verify(clientPair.appClient.responseMock, timeout(500).times(8)).channelRead(any(), any());

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 1 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 2 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 3 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 4 244"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 7 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 30 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 0 89.888037459418 -58.74774244674501"))));
    }

    @Test
    public void testTweetNotWorks() throws Exception {
        reset(blockingIOProcessor);

        clientPair.hardwareClient.send("tweet");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOTIFICATION_INVALID_BODY_EXCEPTION)));

        clientPair.hardwareClient.send("tweet ");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOTIFICATION_INVALID_BODY_EXCEPTION)));

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < 141; i++) {
            a.append("a");
        }

        clientPair.hardwareClient.send("tweet " + a);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NOTIFICATION_INVALID_BODY_EXCEPTION)));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, NOTIFICATION_NOT_AUTHORIZED_EXCEPTION)));
    }

    @Test
    public void testSmsWorks() throws Exception {
        reset(blockingIOProcessor);

        clientPair.hardwareClient.send("sms");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOTIFICATION_INVALID_BODY_EXCEPTION)));

        //no sms widget
        clientPair.hardwareClient.send("sms yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, NOTIFICATION_NOT_AUTHORIZED_EXCEPTION)));

        //adding sms widget
        clientPair.appClient.send("createWidget 1\0{\"id\":432, \"to\":\"3809683423423\", \"x\":0, \"y\":0, \"type\":\"SMS\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("sms yo");
        verify(smsWrapper, timeout(500)).send(eq("3809683423423"), eq("yo"));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.hardwareClient.send("sms yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, QUOTA_LIMIT_EXCEPTION)));
    }

    @Test
    public void testTweetWorks() throws Exception {
        reset(blockingIOProcessor);

        clientPair.hardwareClient.send("tweet yo");
        verify(twitterWrapper, timeout(500)).send(eq("token"), eq("secret"), eq("yo"));

        clientPair.hardwareClient.send("tweet yo");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, QUOTA_LIMIT_EXCEPTION)));
    }

    @Test
    public void testEmailWorks() throws Exception {
        reset(blockingIOProcessor);

        //no email widget
        clientPair.hardwareClient.send("email to subj body");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));

        //adding email widget
        clientPair.appClient.send("createWidget 1\0{\"id\":432, \"x\":0, \"y\":0, \"type\":\"EMAIL\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("email to subj body");
        verify(mailWrapper, timeout(500)).send(eq("to"), eq("subj"), eq("body"));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.hardwareClient.send("email to subj body");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, QUOTA_LIMIT_EXCEPTION)));
    }

    @Test
    public void testWrongCommandForAggregation() throws Exception {
        clientPair.hardwareClient.send("hardware vw 10 aaaa");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 10 aaaa"))));
    }

    @Test
    public void testWrongPin() throws Exception {
        clientPair.hardwareClient.send("hardware vw x aaaa");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));
    }

    @Test
    public void testAppSendWriteHardCommandForGraphAndBack() throws Exception {
        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("ar 7"))));

        String body = "aw 8 333";
        clientPair.hardwareClient.send("hardware " + body);

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        //"aw 11 333".length
        assertEquals(("1 " + body).length(), hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("1 " + body)));
    }

    @Test
    public void testClosedConnectionWhenNotLogged() throws Exception {
        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("getToken 1");
        verify(appClient2.responseMock, after(400).never()).channelRead(any(), any());
        assertTrue(appClient2.isClosed());

        appClient2.send("login dima@mail.ua 1 Android 1RC7");
        verify(appClient2.responseMock, after(200).never()).channelRead(any(), any());
    }

    @Test
    public void testSendPinModeCommandWhenHardwareGoesOnline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.await();

        assertTrue(channelFuture.isDone());

        String body = "pm 13 in";
        clientPair.appClient.send("hardware 1 " + body);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, DEVICE_NOT_IN_NETWORK)));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();
        hardClient.send("login " + clientPair.token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));
        verify(hardClient.responseMock, times(2)).channelRead(any(), any());
        hardClient.stop().awaitUninterruptibly();
    }

    @Test
    public void testSendGeneratedPinModeCommandWhenHardwareGoesOnline() throws Exception {
        ChannelFuture channelFuture = clientPair.hardwareClient.stop();
        channelFuture.awaitUninterruptibly();

        assertTrue(channelFuture.isDone());

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, DEVICE_NOT_IN_NETWORK)));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();
        hardClient.send("login " + clientPair.token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        String expectedBody = "pm 1 out 2 out 3 out 5 out 6 in 7 in 30 in 8 in";
        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b(expectedBody))));
        verify(hardClient.responseMock, times(2)).channelRead(any(), any());
        hardClient.stop().awaitUninterruptibly();
    }

    @Test
    public void testSendHardwareCommandToNotActiveDashboard() throws Exception {
        clientPair.appClient.send("createDash " + "{\"id\":2,\"name\":\"My Dashboard2\"}");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        clientPair.appClient.reset();

        clientPair.appClient.send("getToken 2");

        //getting token for second GetTokenMessage
        ArgumentCaptor<GetTokenMessage> objectArgumentCaptor = ArgumentCaptor.forClass(GetTokenMessage.class);
        verify(clientPair.appClient.responseMock, timeout(2000).times(1)).channelRead(any(), objectArgumentCaptor.capture());
        List<GetTokenMessage> arguments = objectArgumentCaptor.getAllValues();
        GetTokenMessage getTokenMessage = arguments.get(0);
        String token = getTokenMessage.body;

        clientPair.appClient.reset();

        //connecting separate hardware to non active dashboard
        TestHardClient nonActiveDashHardClient = new TestHardClient("localhost", tcpHardPort);
        nonActiveDashHardClient.start();
        nonActiveDashHardClient.send("login " + token);
        verify(nonActiveDashHardClient.responseMock, timeout(2000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        nonActiveDashHardClient.reset();


        //sending hardware command from hardware that has no active dashboard
        nonActiveDashHardClient.send("hardware aw 1 1");
        verify(nonActiveDashHardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NO_ACTIVE_DASHBOARD)));
        verify(clientPair.appClient.responseMock, timeout(1000).times(1)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(1000).times(1)).channelRead(any(), eq(new HardwareConnectedMessage(1, "2")));

        clientPair.hardwareClient.send("hardware aw 1 1");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(0)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 1 1"))));
        nonActiveDashHardClient.stop().awaitUninterruptibly();
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
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("ar 7"))));

        clientPair.hardwareClient.reset();

        clientPair.appClient.send("hardware 1 ar 7");
        verify(clientPair.hardwareClient.responseMock, after(1000).never()).channelRead(any(), any());

        clientPair.appClient.send("hardware 1 ar 6");
        clientPair.appClient.send("hardware 1 ar 6");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(3, HARDWARE, b("ar 6"))));

        clientPair.hardwareClient.reset();

        sleep(100);

        clientPair.appClient.send("hardware 1 ar 6");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(5, HARDWARE, b("ar 6"))));
    }

    @Test
    public void testSendReadCommandsForLCD() throws Exception {
        clientPair.appClient.send("hardware 1 vr 0");
        clientPair.appClient.send("hardware 1 vr 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vr 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vr 1"))));

        clientPair.hardwareClient.reset();
        clientPair.appClient.send("hardware 1 vr 0");
        clientPair.appClient.send("hardware 1 vr 1");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), any());

        sleep(501);
        clientPair.appClient.send("hardware 1 vr 0");
        clientPair.appClient.send("hardware 1 vr 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, HARDWARE, b("vr 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, HARDWARE, b("vr 1"))));
    }

    @Test
    //todo one more test here
    public void test2ClientPairsWorkCorrectly() throws Exception {
        final int ITERATIONS = 100;
        ClientPair clientPair2 = initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, "dima2@mail.ua 1", null, properties);

        String body = "ar 7";
        for (int i = 1; i <= ITERATIONS; i++) {
            clientPair.appClient.send("hardware 1 " + body);
            clientPair2.appClient.send("hardware 1 " + body);
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), any());
        verify(clientPair2.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), any());


        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));
        verify(clientPair2.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));
    }

    @Test
    public void testTryReachQuotaLimit() throws Exception {
        String body = "aw 100 100";

        //within 1 second sending more messages than default limit 100.
        for (int i = 0; i < 200; i++) {
            clientPair.hardwareClient.send("hardware " + body);
            sleep(5);
        }

        ArgumentCaptor<ResponseMessage> objectArgumentCaptor = ArgumentCaptor.forClass(ResponseMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<ResponseMessage> arguments = objectArgumentCaptor.getAllValues();
        ResponseMessage responseMessage = arguments.get(0);
        assertTrue(responseMessage.id > 100);

        //at least 100 iterations should be
        for (int i = 0; i < 100; i++) {
            verify(clientPair.appClient.responseMock).channelRead(any(), eq(produce(i+1, HARDWARE, b("1 " + body))));
        }

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        //check no more accepted
        for (int i = 0; i < 10; i++) {
            clientPair.hardwareClient.send("hardware " + body);
            sleep(9);
        }

        verify(clientPair.hardwareClient.responseMock, times(0)).channelRead(any(), eq(new ResponseMessage(1, QUOTA_LIMIT_EXCEPTION)));
        verify(clientPair.appClient.responseMock, times(0)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));
    }

    @Test
    public void testTimerWidgetTriggered() throws Exception {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                new TimerWorker(holder.userDao, holder.sessionDao), 0, 1000, TimeUnit.MILLISECONDS);

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        Timer timer = new Timer();
        timer.id = 1;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "dw 5 1";
        timer.stopValue = "dw 5 0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        long curTime = localDateTime.getSecond() + localDateTime.getMinute() * 60 + localDateTime.getHour() * 3600;
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 1;
        dashBoard.name = "Test";
        dashBoard.widgets = new Widget[] {timer};

        clientPair.appClient.send("saveDash " + dashBoard.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(7777, HARDWARE, "dw 5 1")));
        clientPair.hardwareClient.reset();
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(7777, HARDWARE, "dw 5 0")));
    }

    @Test
    public void testTimerWidgetTriggeredAndSendCommandToCorrectDevice() throws Exception {
        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                new TimerWorker(holder.userDao, holder.sessionDao), 0, 1000, TimeUnit.MILLISECONDS);

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        Timer timer = new Timer();
        timer.id = 1;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "dw 5 1";
        timer.stopValue = "dw 5 0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        long curTime = localDateTime.getSecond() + localDateTime.getMinute() * 60 + localDateTime.getHour() * 3600;
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 1;
        dashBoard.name = "Test";
        dashBoard.widgets = new Widget[] {timer};

        clientPair.appClient.send("saveDash " + dashBoard.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        dashBoard.id = 2;
        clientPair.appClient.send("createDash " + dashBoard.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getToken 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), any());
        hardClient2.send("login " + clientPair.appClient.getBody());
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        hardClient2.reset();

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(7777, HARDWARE, "dw 5 1")));
        clientPair.hardwareClient.reset();
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(7777, HARDWARE, "dw 5 0")));

        verify(hardClient2.responseMock, never()).channelRead(any(), any());
        hardClient2.stop().awaitUninterruptibly();
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

        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, QUOTA_LIMIT_EXCEPTION)));
        verify(clientPair.hardwareClient.responseMock, atLeast(100)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();

        //waiting to avoid limit.
        sleep(1000);

        for (int i = 0; i < 100000 / 9; i++) {
            clientPair.appClient.send("hardware " + body, 1);
            sleep(9);
        }

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, QUOTA_LIMIT_EXCEPTION)));
        verify(clientPair.hardwareClient.responseMock, atLeast(100)).channelRead(any(), eq(produce(1, HARDWARE, b(body))));

    }

    @Override
    public String getDataFolder() {
        return com.google.common.io.Files.createTempDir().toString();
    }
}
