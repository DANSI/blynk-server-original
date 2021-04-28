package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.ui.TimeInput;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static cc.blynk.integration.TestUtil.appSync;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.setProperty;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.after;
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
public class SyncWorkflowTest extends SingleServerInstancePerTest {

    @Test
    public void testHardSyncReturnHardwareCommands() throws Exception {
        clientPair.hardwareClient.sync();
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(8)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
    }

    @Test
    public void testHardSyncReturnNoSetPropertyCommands() throws Exception {
        clientPair.hardwareClient.setProperty(44, "label", "hello");
        clientPair.hardwareClient.verifyResult(ok(1));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(setProperty(1, "1-0 44 label hello")));
        clientPair.hardwareClient.reset();

        testHardSyncReturnHardwareCommands();
    }

    @Test
    public void testHardSyncReturnNothingNoWidgetOnPin() throws Exception {
        clientPair.hardwareClient.sync(PinType.VIRTUAL, 22);
        verify(clientPair.hardwareClient.responseMock, after(400).never()).channelRead(any(), any());
    }

    @Test
    public void testHardSyncReturnValueForNoWidgetOnVirtualPin() throws Exception {
        clientPair.hardwareClient.send("hardware vw 67 100");

        clientPair.hardwareClient.sync(PinType.VIRTUAL, 67);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 67 100"))));

        clientPair.hardwareClient.reset();

        clientPair.hardwareClient.sync();
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(9)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 67 100"))));
    }

    @Test
    public void testHardSyncReturnValueForNoWidgetOnAnalogPin() throws Exception {
        clientPair.hardwareClient.send("hardware aw 66 100");

        clientPair.hardwareClient.sync(PinType.ANALOG, 66);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("aw 66 100"))));

        clientPair.hardwareClient.reset();

        clientPair.hardwareClient.sync();
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(9)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 66 100"))));
    }

    @Test
    public void testHardSyncReturn1HardwareCommand() throws Exception {
        clientPair.hardwareClient.sync(PinType.VIRTUAL, 4);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
    }

    @Test
    public void testLCDOnActivateSendsCorrectBodySimpleMode() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"LCD\",\"id\":1923810267,\"x\":0,\"y\":6,\"color\":600084223,\"width\":8,\"height\":2,\"tabId\":0,\"" +
                "pins\":[" +
                "{\"pin\":10,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"10\"}," +
                "{\"pin\":11,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"11\"}]," +
                "\"advancedMode\":false,\"textLight\":false,\"textLightOn\":false,\"frequency\":1000}");

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();
        clientPair.appClient.activate(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(13)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync(b("1-0 vw 10 10")));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 11 11"))));


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
    }

    @Test
    public void testLCDOnActivateSendsCorrectBodyAdvancedMode() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"LCD\",\"id\":1923810267,\"x\":0,\"y\":6,\"color\":600084223,\"width\":8,\"height\":2,\"tabId\":0,\"" +
                "pins\":[" +
                "{\"pin\":10,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023}," +
                "{\"pin\":11,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023}]," +
                "\"advancedMode\":true,\"textLight\":false,\"textLightOn\":false,\"frequency\":1000}");

        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 10 p x y 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("1-0 vw 10 p x y 10"))));

        clientPair.appClient.reset();
        clientPair.appClient.activate(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 10 p x y 10"))));


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
    }

    @Test
    public void testHardSyncReturnRTCWithoutTimezone() throws Exception {
        clientPair.hardwareClient.send("internal rtc");

        long expectedTS = System.currentTimeMillis() / 1000;

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(Command.BLYNK_INTERNAL, hardMessage.command);
        assertEquals(14, hardMessage.body.length());
        String tsString = hardMessage.body.split("\0")[1];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 7200 + 100);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezone() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"RTC\",\"id\":99, " +
                "\"x\":0,\"y\":0,\"width\":2,\"height\":1}");

        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("internal rtc");

        long expectedTS = System.currentTimeMillis() / 1000;

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(Command.BLYNK_INTERNAL, hardMessage.command);
        assertEquals(14, hardMessage.body.length());
        String tsString = hardMessage.body.split("\0")[1];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 7200 + 100);
    }

    @Test(expected = DateTimeException.class)
    public void testWrongAsiaTimeZone() {
        ZoneId.of("Asia/Hanoi");
    }

    @Test
    public void testCorrectAsiaTimeZone() {
        ZoneId.of("Asia/Ho_Chi_Minh");
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezonePlus3() throws Exception {
        ZoneId zoneId = ZoneId.of("Europe/Kiev");

        clientPair.appClient.createWidget(1, "{\"type\":\"RTC\",\"id\":99, " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1," +
                "\"tzName\":\"TZ\"}".replace("TZ", zoneId.toString()));

        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("internal rtc");

        int offset = LocalDateTime.now().atZone(zoneId).getOffset().getTotalSeconds();

        long expectedTS = System.currentTimeMillis() / 1000 + LocalDateTime.now().atZone(zoneId).getOffset().getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(Command.BLYNK_INTERNAL, hardMessage.command);
        assertEquals(14, hardMessage.body.length());
        String tsString = hardMessage.body.split("\0")[1];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, offset + 100);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezoneMinus3() throws Exception {
        ZoneId zoneId = ZoneId.of("Brazil/East");

        clientPair.appClient.createWidget(1, "{\"type\":\"RTC\",\"id\":99, " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1," +
                "\"tzName\":\"TZ\"}".replace("TZ", zoneId.toString()));

        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("internal rtc");

        long expectedTS = System.currentTimeMillis() / 1000 + LocalDateTime.now().atZone(zoneId).getOffset().getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(Command.BLYNK_INTERNAL, hardMessage.command);
        assertEquals(14, hardMessage.body.length());
        String tsString = hardMessage.body.split("\0")[1];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, -LocalDateTime.now().atZone(zoneId).getOffset().getTotalSeconds() + 100);
    }


    @Test
    public void testHardSyncForTimeInputWidget() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"TIME_INPUT\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":2,\"height\":1}");

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1-0 vw " + b("99 82800 82860 Europe/Kiev 1"));
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 99 82800 82860 Europe/Kiev 1"))));

        clientPair.hardwareClient.sync(PinType.VIRTUAL, 99);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 99 82800 82860 Europe/Kiev 1"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        TimeInput timeInput = (TimeInput) profile.dashBoards[0].findWidgetByPin(0, (short) 99, PinType.VIRTUAL);
        assertNotNull(timeInput);
        assertEquals(82800, timeInput.startAt);
        assertEquals(82860, timeInput.stopAt);
        assertEquals(ZoneId.of("Europe/Kiev"), timeInput.tzName);
        assertArrayEquals(new int[] {1}, timeInput.days);
    }

    @Test
    public void testSyncForTimer() throws Exception {
        User user = holder.userDao.users.get(new UserKey(getUserName(), "Blynk"));
        Widget widget = user.profile.dashBoards[0].findWidgetByPin(0, (short) 5, PinType.DIGITAL);
        Timer timer = (Timer) widget;
        timer.value = "100500";

        clientPair.hardwareClient.sync(PinType.DIGITAL, 5);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 100500"))));

        Thread thread = new Thread(() -> {
            timer.value = "200300";
        });

        thread.start();
        thread.join();

        clientPair.hardwareClient.sync(PinType.DIGITAL, 5);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("dw 5 200300"))));

        clientPair.hardwareClient.reset();

        clientPair.hardwareClient.sync();
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(8)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 200300"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
    }



    @Test
    public void testTerminalSendsSyncOnActivate() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "7500")));

        clientPair.appClient.createWidget(1, "{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":17}");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.send("hardware vw 17 a");
        clientPair.hardwareClient.send("hardware vw 17 b");
        clientPair.hardwareClient.send("hardware vw 17 c");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 17 a"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 17 b"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("1-0 vw 17 c"))));

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(5));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vm 17 a b c"))));
    }

    @Test
    public void testLCDSendsSyncOnActivate() throws Exception {
        clientPair.hardwareClient.send("hardware vw 20 p 0 0 Hello");
        clientPair.hardwareClient.send("hardware vw 20 p 0 1 World");

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 20 p 0 0 Hello"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 20 p 0 1 World"))));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 0 Hello"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 1 World"))));
    }

    @Test
    public void testLCDSendsSyncOnActivate2() throws Exception {
        clientPair.hardwareClient.send("hardware vw 20 p 0 0 H1");
        clientPair.hardwareClient.send("hardware vw 20 p 0 1 H2");
        clientPair.hardwareClient.send("hardware vw 20 p 0 2 H3");
        clientPair.hardwareClient.send("hardware vw 20 p 0 3 H4");
        clientPair.hardwareClient.send("hardware vw 20 p 0 4 H5");
        clientPair.hardwareClient.send("hardware vw 20 p 0 5 H6");
        clientPair.hardwareClient.send("hardware vw 20 p 0 6 H7");

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 20 p 0 0 H1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 20 p 0 1 H2"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("1-0 vw 20 p 0 2 H3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, HARDWARE, b("1-0 vw 20 p 0 3 H4"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, HARDWARE, b("1-0 vw 20 p 0 4 H5"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, HARDWARE, b("1-0 vw 20 p 0 5 H6"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7, HARDWARE, b("1-0 vw 20 p 0 6 H7"))));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 1 H2"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 2 H3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 3 H4"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 4 H5"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 5 H6"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(appSync(b("1-0 vw 20 p 0 6 H7"))));
    }


    @Test
    public void testSyncWorksForGauge() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":155, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":100}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 100 101");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("1-0 vw 100 101"))));

        clientPair.hardwareClient.sync(PinType.VIRTUAL, 100);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("vw 100 101"))));
    }

    @Test
    public void testSyncForMultiPins() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":155, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"GAUGE\", \"pinType\":\"VIRTUAL\", \"pin\":100}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 100 100");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("1-0 vw 100 100"))));

        clientPair.hardwareClient.send("hardware vw 101 101");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("1-0 vw 101 101"))));

        clientPair.hardwareClient.sync(PinType.VIRTUAL, 100, 101);
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(3, b("vw 100 100"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(3, b("vw 101 101"))));
    }

    @Test
    public void testActivateAndGetSync() throws Exception {
        clientPair.appClient.activate(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(11)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

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
    }

    @Test
    public void testSyncForMultiDevices() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":188, \"width\":1, \"height\":1, \"deviceId\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":4, \"value\":1}");
        clientPair.appClient.verifyResult(ok(1));

        Device device1 = new Device(1, "My Device", BoardType.ESP8266);

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice(2);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(2, device)));

        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        hardClient2.login(device.token);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();

        clientPair.hardwareClient.sync();
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(8)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));

        hardClient2.sync();
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 1"))));
    }

    @Test
    public void testSyncForMultiDevicesNoWidget() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        hardClient2.login(device.token);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE_CONNECTED, "1-1")));

        clientPair.hardwareClient.send("hardware vw 119 1");
        hardClient2.send("hardware vw 119 1");

        clientPair.hardwareClient.sync();
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(9)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 13 60 143 158"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 119 1"))));

        hardClient2.sync();
        verify(hardClient2.responseMock, timeout(100)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 119 1"))));
    }

    @Test
    public void testHardSyncSinglePinFor2DEvices() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP32_Dev_Board);
        Device device2 = new Device(2, "My Device2", BoardType.ESP32_Dev_Board);

        DashBoard dash = new DashBoard();
        dash.id = 2;
        dash.name = "123";
        dash.isActive = true;

        clientPair.appClient.createDash(dash);
        clientPair.appClient.verifyResult(ok(1));

        Device tempDevice1;
        clientPair.appClient.createDevice(1, device1);
        tempDevice1 = clientPair.appClient.parseDevice(2);
        assertNotNull(tempDevice1);
        assertNotNull(tempDevice1.token);
        clientPair.appClient.verifyResult(createDevice(2, tempDevice1));

        Device tempDevice2;
        clientPair.appClient.createDevice(2, device2);
        tempDevice2 = clientPair.appClient.parseDevice(3);
        assertNotNull(tempDevice2);
        assertNotNull(tempDevice2.token);
        clientPair.appClient.verifyResult(createDevice(3, tempDevice2));

        //set pin state from the app
        clientPair.appClient.send("hardware 1-1 vw 44 444");
        clientPair.appClient.send("hardware 2-2 vw 44 445");

        TestHardClient hardClient1 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient1.start();
        hardClient1.login(tempDevice1.token);
        hardClient1.verifyResult(ok(1));
        hardClient1.reset();

        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();
        hardClient2.login(tempDevice2.token);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();

        hardClient1.sync(PinType.VIRTUAL, 44);
        hardClient1.verifyResult(produce(1, HARDWARE, b("vw 44 444")));

        hardClient2.sync(PinType.VIRTUAL, 44);
        hardClient2.verifyResult(produce(1, HARDWARE, b("vw 44 445")));

        hardClient1.send("hardware vw 45 555");
        hardClient2.send("hardware vw 45 556");

        hardClient1.sync(PinType.VIRTUAL, 45);
        hardClient1.verifyResult(produce(3, HARDWARE, b("vw 45 555")));

        hardClient2.sync(PinType.VIRTUAL, 45);
        hardClient2.verifyResult(produce(3, HARDWARE, b("vw 45 556")));
    }

}
