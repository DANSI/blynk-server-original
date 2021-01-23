package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.TimerTime;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinActionType;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.notification.NotifyAction;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.model.widgets.ui.tiles.templates.ButtonTileTemplate;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.enums.Priority;
import cc.blynk.utils.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.createTag;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.model.serialization.JsonParser.MAPPER;
import static cc.blynk.server.workers.timer.TimerWorker.TIMER_MSG_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.after;
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
public class TimerTest extends SingleServerInstancePerTest {

    private ScheduledExecutorService ses;

    @Before
    public void initSES() {
        ses = Executors.newScheduledThreadPool(1);
    }

    @After
    public void closeSES() {
        ses.shutdownNow();
    }


    @Test
    public void testTimerEvent() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        TimerTime timerTime = new TimerTime(
                0,
                new int[] {1,2,3,4,5,6,7},
                //adding 2 seconds just to be sure we no gonna miss timer event
                LocalTime.now(DateTimeUtils.UTC).toSecondOfDay() + 2,
                DateTimeUtils.UTC
        );


        DataStream dataStream = new DataStream((short) 1,PinType.VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "1", SetPinActionType.CUSTOM);

        Eventor eventor = new Eventor(new Rule[] {
                new Rule(dataStream, timerTime, null, new BaseAction[] {setPinAction}, true)
        });

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, timeout(3000)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "1-0 vw 1 1")));
        verify(clientPair.hardwareClient.responseMock, timeout(3000)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "vw 1 1")));
    }


    @Test
    public void testTimerEventNotActive() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        TimerTime timerTime = new TimerTime(
                0,
                new int[] {1,2,3,4,5,6,7},
                //adding 2 seconds just to be sure we no gonna miss timer event
                LocalTime.now(DateTimeUtils.UTC).toSecondOfDay() + 2,
                DateTimeUtils.UTC
        );


        DataStream dataStream = new DataStream((short)1,PinType.VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "1", SetPinActionType.CUSTOM);

        Eventor eventor = new Eventor(new Rule[] {
                new Rule(dataStream, timerTime, null, new BaseAction[] {setPinAction}, true)
        });

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, timeout(3000)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "1-0 vw 1 1")));
        verify(clientPair.hardwareClient.responseMock, timeout(3000)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "vw 1 1")));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();


        eventor = new Eventor(new Rule[] {
                new Rule(dataStream, new TimerTime(
                        0,
                        new int[] {1,2,3,4,5,6,7},
                        //adding 2 seconds just to be sure we no gonna miss timer event
                        LocalTime.now(DateTimeUtils.UTC).toSecondOfDay() + 1,
                        DateTimeUtils.UTC
                ),
                        null, new BaseAction[] {setPinAction}, false)
        });

        clientPair.appClient.updateWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, after(1500).never()).channelRead(any(), eq(hardware(TIMER_MSG_ID, "1-0 vw 1 1")));
        verify(clientPair.hardwareClient.responseMock, after(1500).never()).channelRead(any(), eq(hardware(TIMER_MSG_ID, "vw 1 1")));
    }

    @Test
    public void testTimerEventWithMultiActions() throws Exception {
        TimerTime timerTime = new TimerTime(
                0,
                new int[] {1,2,3,4,5,6,7},
                //adding 2 seconds just to be sure we no gonna miss timer event
                LocalTime.now(DateTimeUtils.UTC).toSecondOfDay() + 2,
                DateTimeUtils.UTC
        );


        DataStream dataStream = new DataStream((short)1,PinType.VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "1", SetPinActionType.CUSTOM);

        DataStream dataStream2 = new DataStream((short)2,PinType.VIRTUAL);
        SetPinAction setPinAction2 = new SetPinAction(dataStream2, "2", SetPinActionType.CUSTOM);

        Rule rule = new Rule(null, timerTime, null, new BaseAction[] {setPinAction, setPinAction2}, true);

        Eventor eventor = new Eventor(new Rule[] {
                rule
        });

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        verify(clientPair.appClient.responseMock, timeout(2100)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "1-0 vw 1 1")));
        verify(clientPair.appClient.responseMock, timeout(2100)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "1-0 vw 2 2")));
        verify(clientPair.hardwareClient.responseMock, timeout(2100)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "vw 1 1")));
        verify(clientPair.hardwareClient.responseMock, timeout(2100)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "vw 2 2")));
    }

    @Test
    public void testTimerEventWithMultiActions1() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        TimerTime timerTime = new TimerTime(
                0,
                new int[] {1,2,3,4,5,6,7},
                //adding 2 seconds just to be sure we no gonna miss timer event
                LocalTime.now(DateTimeUtils.UTC).toSecondOfDay() + 2,
                DateTimeUtils.UTC
        );

        DataStream dataStream = new DataStream((short) 1,PinType.VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "1", SetPinActionType.CUSTOM);
        NotifyAction notifyAction = new NotifyAction("Hello");
        Rule rule = new Rule(null, timerTime, null, new BaseAction[] {setPinAction, notifyAction}, true);

        Eventor eventor = new Eventor(new Rule[] {
                rule
        });

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        ArgumentCaptor<AndroidGCMMessage> objectArgumentCaptor = ArgumentCaptor.forClass(AndroidGCMMessage.class);
        verify(holder.gcmWrapper, timeout(2000).times(1)).send(objectArgumentCaptor.capture(), any(), any());
        AndroidGCMMessage message = objectArgumentCaptor.getValue();

        String expectedJson = new AndroidGCMMessage("token", Priority.normal, "Hello", 1).toJson();
        assertEquals(expectedJson, message.toJson());

        verify(clientPair.appClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "1-0 vw 1 1")));
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(TIMER_MSG_ID, "vw 1 1")));
    }

    @Test
    public void testIsTimeMethod() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(DateTimeUtils.UTC).withHour(23);
        //kiev is +2, so as currentDateTime has 23 hour. kiev should be always ahead.
        LocalDateTime userDateTime = currentDateTime.withZoneSameInstant(ZoneId.of("Europe/Kiev")).toLocalDateTime();
        assertNotEquals(currentDateTime.getDayOfWeek(), userDateTime.getDayOfWeek());
    }

    @Test
    public void testTimerEventWithWrongDayDontWork() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        ZonedDateTime now = ZonedDateTime.now(DateTimeUtils.UTC);
        int currentDayIndex = now.getDayOfWeek().ordinal();

        int[] days = new int[] {1,2,3,4,5,6,7};
        //removing today day from expected days so timer doesnt work.
        days[currentDayIndex] = -1;

        TimerTime timerTime = new TimerTime(
                0,
                days,
                //adding 2 seconds just to be sure we no gonna miss timer event
                LocalTime.now(DateTimeUtils.UTC).toSecondOfDay() + 1,
                DateTimeUtils.UTC
        );

        DataStream dataStream = new DataStream((short)1,PinType.VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "1", SetPinActionType.CUSTOM);
        Rule rule = new Rule(null, timerTime, null,  new BaseAction[] {setPinAction}, true);

        Eventor eventor = new Eventor(new Rule[] {
                rule
        });

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, after(700).never()).channelRead(any(), eq(hardware(TIMER_MSG_ID, "1-0 vw 1 1")));
        verify(clientPair.hardwareClient.responseMock, after(700).never()).channelRead(any(), eq(hardware(TIMER_MSG_ID, "vw 1 1")));
    }

    @Test
    public void testAddTimerWidgetWithStartTimeTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.width = 2;
        timer.height = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.hardwareClient.responseMock, timeout(1500).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
    }

    @Test
    public void testAddTimerWidgetWithStopAndStartTimeTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.width = 2;
        timer.height = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.stopTime = curTime + 1;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.hardwareClient.responseMock, timeout(1500).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 0")));
    }

    @Test
    public void testAddTimerWidgetWithStopTimeTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.width = 2;
        timer.height = 1;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.hardwareClient.responseMock, timeout(2500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 0")));
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
    }


    @Test
    public void testAddTimerWidgetWithStopTimeAndRemove() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.width = 2;
        timer.height = 1;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.deleteWidget(1, 112);
        clientPair.appClient.verifyResult(ok(2));

        verify(clientPair.hardwareClient.responseMock, after(2500).never()).channelRead(any(), any());
    }

    @Test
    public void testAddFewTimersWidgetWithStartTimeTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.width = 2;
        timer.height = 1;
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        timer.id = 113;
        timer.startValue = "2";

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(2));

        verify(clientPair.hardwareClient.responseMock, timeout(2500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 2")));
    }

    @Test
    public void testAddTimerWithSameStartStopTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.width = 2;
        timer.height = 1;
        timer.startValue = "0";
        timer.stopValue = "1";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 1;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.hardwareClient.responseMock, timeout(2500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 0")));
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
    }

    @Test
    public void testUpdateTimerWidgetWithStopTimeTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.width = 2;
        timer.height = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        timer.startValue = "11";
        timer.stopValue = "10";

        clientPair.appClient.updateWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.hardwareClient.responseMock, timeout(2500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 11")));
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 10")));
    }

    @Test
    public void testStopTimerTrigger() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.width = 2;
        timer.height = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        timer.startTime = -1;
        timer.stopTime = -1;

        clientPair.appClient.updateWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.hardwareClient.responseMock, after(2500).times(0)).channelRead(any(), any());
        //verify(clientPair.hardwareClient.responseMock, timeout(2500).times(2)).channelRead(any(), any());
        //verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 11")));
        //verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 10")));
    }

    @Test
    public void testDashTimerNotTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.width = 2;
        timer.height = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.deleteDash(1);
        clientPair.appClient.verifyResult(ok(2));

        verify(clientPair.hardwareClient.responseMock, after(2500).times(0)).channelRead(any(), any());
    }



    @Test
    public void testTimerWidgetTriggeredAndSendCommandToCorrectDevice() throws Exception {
        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        Timer timer = new Timer();
        timer.id = 1;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 1;
        dashBoard.name = "Test";
        dashBoard.widgets = new Widget[] {timer};

        clientPair.appClient.updateDash(dashBoard);
        clientPair.appClient.verifyResult(ok(2));

        dashBoard.id = 2;
        clientPair.appClient.createDash(dashBoard);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.reset();
        clientPair.appClient.createDevice(2, new Device(1, "Device", BoardType.ESP8266));
        Device device = clientPair.appClient.parseDevice();
        hardClient2.login(device.token);
        hardClient2.verifyResult(ok(1));
        hardClient2.reset();

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
        clientPair.hardwareClient.reset();
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 0")));

        verify(hardClient2.responseMock, never()).channelRead(any(), any());
        hardClient2.stop().awaitUninterruptibly();
    }

    @Test
    public void testTimerWidgetTriggered() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        Timer timer = new Timer();
        timer.id = 1;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 1;
        dashBoard.name = "Test";
        dashBoard.widgets = new Widget[] {timer};

        clientPair.appClient.updateDash(dashBoard);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(3));

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
        clientPair.hardwareClient.reset();
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 0")));
    }

    @Test
    public void testTimerWorksWithTag() throws Exception {
        //creating new device
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
        clientPair.appClient.reset();

        //creating new tag
        Tag tag0 = new Tag(100_000, "Tag1");
        //assigning 2 devices on 1 tag.
        tag0.deviceIds = new int[] {0, 1};

        clientPair.appClient.createTag(1, tag0);
        String createdTag = clientPair.appClient.getBody();
        Tag tag = JsonParser.parseTag(createdTag, 0);
        assertNotNull(tag);
        assertEquals(100_000, tag.id);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createTag(1, tag)));

        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.width = 2;
        timer.height = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.deviceId = 100_000;

        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(2));

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
        verify(hardClient2.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
    }

    @Test
    public void testTimerWidgetTriggeredAndSyncWorks() throws Exception {
        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        Timer timer = new Timer();
        timer.id = 1;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = PinType.VIRTUAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 2;

        DashBoard dashBoard = new DashBoard();
        dashBoard.id = 1;
        dashBoard.name = "Test";
        dashBoard.widgets = new Widget[] {timer};

        clientPair.appClient.updateDash(dashBoard);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(3));

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "vw 5 1")));
        clientPair.hardwareClient.reset();
        clientPair.hardwareClient.sync(PinType.VIRTUAL, 5);
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(1, "vw 5 1")));

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "vw 5 0")));

        clientPair.hardwareClient.sync(PinType.VIRTUAL, 5);
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(2, "vw 5 0")));
    }

    @Test
    public void testAddTimerWidgetToDeviceTilesWithStartTimeTriggered() throws Exception {
        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new ButtonTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 111, PinType.VIRTUAL),
                false, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + deviceTiles.id + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        ses.scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.width = 2;
        timer.height = 1;
        timer.pinType = PinType.DIGITAL;
        timer.pin = 5;
        timer.startValue = "1";
        timer.deviceId = -1;
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;

        clientPair.appClient.createWidget(1, b("21321 1 ") + JsonParser.MAPPER.writeValueAsString(timer));
        clientPair.appClient.verifyResult(ok(3));

        verify(clientPair.hardwareClient.responseMock, timeout(1500).times(1)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "dw 5 1")));
        verify(clientPair.appClient.responseMock, timeout(2000)).channelRead(any(), eq(hardware(7777, "1-0 dw 5 1")));
    }
}
