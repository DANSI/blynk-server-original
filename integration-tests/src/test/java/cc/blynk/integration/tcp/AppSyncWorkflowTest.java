package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.ValueDisplay;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.table.Table;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.model.widgets.ui.tiles.templates.PageTileTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.appSync;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.setProperty;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AppSyncWorkflowTest extends SingleServerInstancePerTest {

    @Test
    public void testLCDOnActivateSendsCorrectBodySimpleMode() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"LCD\",\"id\":1923810267,\"x\":0,\"y\":6,\"color\":600084223,\"width\":8,\"height\":2,\"tabId\":0,\"" +
                "pins\":[" +
                "{\"pin\":10,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"10\"}," +
                "{\"pin\":11,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"11\"}]," +
                "\"advancedMode\":false,\"textLight\":false,\"textLightOn\":false,\"frequency\":1000}");

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(13)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 vw 10 10"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 11"));


        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
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
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 10 p x y 10"));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync(1111, "1-0 vw 10 p x y 10"));


        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
    }


    @Test
    public void testTerminalSendsSyncOnActivate() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(2, GET_ENERGY, "7500"));

        clientPair.appClient.createWidget(1, "{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":17}");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.send("hardware vw 17 a");
        clientPair.hardwareClient.send("hardware vw 17 b");
        clientPair.hardwareClient.send("hardware vw 17 c");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 17 a"));
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 17 b"));
        clientPair.appClient.verifyResult(hardware(3, "1-0 vw 17 c"));

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(5));
        clientPair.appClient.verifyResult(appSync("1-0 vm 17 a b c"));
    }

    @Test
    public void testTerminalStorageRemembersCommands() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        clientPair.appClient.verifyResult(produce(2, GET_ENERGY, "7500"));

        clientPair.appClient.createWidget(1, "{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":17}");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.send("hardware vw 17 1");
        clientPair.hardwareClient.send("hardware vw 17 2");
        clientPair.hardwareClient.send("hardware vw 17 3");
        clientPair.hardwareClient.send("hardware vw 17 4");
        clientPair.hardwareClient.send("hardware vw 17 dddyyyiii");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(hardware(5, "1-0 vw 17 dddyyyiii")));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(appSync("1-0 vm 17 1 2 3 4 dddyyyiii"));
    }

    @Test
    public void testTerminalStorageRemembersCommandsInNewFormat() throws Exception {
        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        appClient.createWidget(1, "{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(4));

        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 56 2");
        clientPair.hardwareClient.send("hardware vw 56 3");
        clientPair.hardwareClient.send("hardware vw 56 4");
        clientPair.hardwareClient.send("hardware vw 56 dddyyyiii");

        appClient.verifyResult(hardware(1, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 4"));
        appClient.verifyResult(hardware(5, "1-0 vw 56 dddyyyiii"));

        appClient.activate(1);
        appClient.verifyResult(ok(5));

        appClient.verifyResult(appSync("1-0 vm 56 1 2 3 4 dddyyyiii"));
    }

    @Test
    public void testTableStorageRemembersCommandsInNewFormat() throws Exception {
        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        Table table = new Table();
        table.id = 102;
        table.width = 2;
        table.height = 2;
        table.pin = 56;
        table.pinType = PinType.VIRTUAL;
        table.deviceId = 0;

        appClient.createWidget(1, table);
        appClient.verifyResult(ok(3));

        clientPair.hardwareClient.send("hardware vw 56 add 1 Row1 row1");
        clientPair.hardwareClient.send("hardware vw 56 add 2 Row2 row2");
        clientPair.hardwareClient.send("hardware vw 56 add 3 Row3 row3");
        clientPair.hardwareClient.send("hardware vw 56 add 4 Row4 row4");

        appClient.verifyResult(hardware(1, "1-0 vw 56 add 1 Row1 row1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 add 2 Row2 row2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 add 3 Row3 row3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 add 4 Row4 row4"));

        appClient.activate(1);
        appClient.verifyResult(ok(4));

        appClient.verifyResult(appSync("1-0 vm 56 add 1 Row1 row1 true add 2 Row2 row2 true add 3 Row3 row3 true add 4 Row4 row4 true"));
    }

    @Test
    public void testTerminalAndAnotherWidgetOnTheSamePin() throws Exception {
        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        appClient.createWidget(1, "{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.createWidget(1, "{\"id\":103, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(4));
        appClient.verifyResult(ok(5));

        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 56 2");
        clientPair.hardwareClient.send("hardware vw 56 3");
        clientPair.hardwareClient.send("hardware vw 56 4");
        clientPair.hardwareClient.send("hardware vw 56 dddyyyiii");

        appClient.verifyResult(hardware(1, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 4"));
        appClient.verifyResult(hardware(5, "1-0 vw 56 dddyyyiii"));

        appClient.activate(1);
        appClient.verifyResult(ok(6));

        appClient.verifyResult(appSync("1-0 vm 56 1 2 3 4 dddyyyiii"));
        appClient.verifyResult(appSync("1-0 vw 56 dddyyyiii"));
    }

    @Test
    public void testTerminalAndAnotherWidgetOnTheSamePinAndDeviceSelector() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        DeviceSelector deviceSelector = new DeviceSelector();
        deviceSelector.id = 200_000;
        deviceSelector.height = 4;
        deviceSelector.width = 1;
        deviceSelector.deviceIds = new int [] {0, 1};

        appClient.createWidget(1, deviceSelector);
        appClient.verifyResult(ok(4));

        appClient.createWidget(1, "{\"id\":103, \"deviceId\":200000, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(5));
        appClient.createWidget(1, "{\"id\":102, \"deviceId\":200000, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(6));

        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 56 2");
        clientPair.hardwareClient.send("hardware vw 56 3");
        clientPair.hardwareClient.send("hardware vw 56 4");
        clientPair.hardwareClient.send("hardware vw 56 dddyyyiii");

        appClient.verifyResult(hardware(1, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 4"));
        appClient.verifyResult(hardware(5, "1-0 vw 56 dddyyyiii"));

        appClient.sync(1, 0);
        appClient.verifyResult(ok(7));

        appClient.verifyResult(appSync("1-0 vw 56 dddyyyiii"));
        appClient.verifyResult(appSync("1-0 vm 56 1 2 3 4 dddyyyiii"));
    }

    @Test
    public void testTerminalAndAnotherWidgetOnTheSamePinAndDeviceSelectorAnotherOrder() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        DeviceSelector deviceSelector = new DeviceSelector();
        deviceSelector.id = 200_000;
        deviceSelector.height = 4;
        deviceSelector.width = 1;
        deviceSelector.deviceIds = new int [] {0, 1};

        appClient.createWidget(1, deviceSelector);
        appClient.verifyResult(ok(4));

        appClient.createWidget(1, "{\"id\":102, \"deviceId\":200000, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(5));
        appClient.createWidget(1, "{\"id\":103, \"deviceId\":200000, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"DIGIT4_DISPLAY\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(6));

        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 56 2");
        clientPair.hardwareClient.send("hardware vw 56 3");
        clientPair.hardwareClient.send("hardware vw 56 4");
        clientPair.hardwareClient.send("hardware vw 56 dddyyyiii");

        appClient.verifyResult(hardware(1, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 4"));
        appClient.verifyResult(hardware(5, "1-0 vw 56 dddyyyiii"));

        appClient.sync(1, 0);
        appClient.verifyResult(ok(7));

        appClient.verifyResult(appSync("1-0 vw 56 dddyyyiii"));
        appClient.verifyResult(appSync("1-0 vm 56 1 2 3 4 dddyyyiii"));
    }

    @Test
    public void testTerminalStorageRemembersCommandsInOldFormatAndDeviceSelector() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.25.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        DeviceSelector deviceSelector = new DeviceSelector();
        deviceSelector.id = 200_000;
        deviceSelector.height = 4;
        deviceSelector.width = 1;
        deviceSelector.deviceIds = new int [] {0, 1};

        appClient.createWidget(1, deviceSelector);
        appClient.verifyResult(ok(4));

        appClient.createWidget(1, "{\"id\":102, \"deviceId\":200000, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(5));

        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 56 2");
        clientPair.hardwareClient.send("hardware vw 56 3");
        clientPair.hardwareClient.send("hardware vw 56 4");
        clientPair.hardwareClient.send("hardware vw 56 dddyyyiii");

        appClient.verifyResult(hardware(1, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 4"));
        appClient.verifyResult(hardware(5, "1-0 vw 56 dddyyyiii"));

        appClient.sync(1, 0);
        appClient.verifyResult(ok(6));

        appClient.verifyResult(appSync("1-0 vm 56 1 2 3 4 dddyyyiii"));
    }

    @Test
    public void testTerminalStorageRemembersCommandsInNewFormatAndDeviceTiles() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        appClient.createWidget(1, deviceTiles);
        appClient.verifyResult(ok(4));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        appClient.verifyResult(ok(5));

        appClient.createWidget(1, deviceTiles.id, tileTemplate.id, "{\"id\":102, \"deviceId\":-1, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(6));

        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 56 2");
        clientPair.hardwareClient.send("hardware vw 56 3");
        clientPair.hardwareClient.send("hardware vw 56 4");
        clientPair.hardwareClient.send("hardware vw 56 dddyyyiii");

        appClient.verifyResult(hardware(1, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 4"));
        appClient.verifyResult(hardware(5, "1-0 vw 56 dddyyyiii"));

        appClient.sync(1, 0);
        appClient.verifyResult(ok(7));

        appClient.verifyResult(appSync("1-0 vm 56 1 2 3 4 dddyyyiii"));
    }

    @Test
    public void testTerminalStorageCleanedAfterTilesAreRemoved() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        appClient.createWidget(1, deviceTiles);
        appClient.verifyResult(ok(4));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        appClient.verifyResult(ok(5));

        appClient.createWidget(1, deviceTiles.id, tileTemplate.id, "{\"id\":102, \"deviceId\":-1, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(6));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 103;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.deviceId = -1;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.pin = 57;

        appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        appClient.verifyResult(ok(7));

        clientPair.hardwareClient.send("hardware vw 1 0");
        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 57 2");

        appClient.verifyResult(hardware(1, "1-0 vw 1 0"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(3, "1-0 vw 57 2"));

        appClient.sync(1, 0);
        appClient.verifyResult(ok(8));

        appClient.verifyResult(appSync("1-0 vm 56 1"));
        appClient.verifyResult(appSync("1-0 vw 1 0"));
        appClient.verifyResult(appSync("1-0 vw 57 2"));

        appClient.deleteWidget(1, deviceTiles.id);
        appClient.verifyResult(ok(9));

        appClient.reset();
        appClient.sync(1, 0);
        appClient.verifyResult(ok(1));

        appClient.neverAfter(100, appSync("1-0 vm 56 1"));
        appClient.neverAfter(100, appSync("1-0 vw 1 0"));
        appClient.neverAfter(100, appSync("1-0 vw 57 2"));

    }

    @Test
    public void testTerminalStorageRemembersCommandsInNewFormatAndDeviceSelector() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        DeviceSelector deviceSelector = new DeviceSelector();
        deviceSelector.id = 200_000;
        deviceSelector.height = 4;
        deviceSelector.width = 1;
        deviceSelector.deviceIds = new int [] {0, 1};

        appClient.createWidget(1, deviceSelector);
        appClient.verifyResult(ok(4));

        appClient.createWidget(1, "{\"id\":102, \"deviceId\":200000, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":56}");
        appClient.verifyResult(ok(5));

        clientPair.hardwareClient.send("hardware vw 56 1");
        clientPair.hardwareClient.send("hardware vw 56 2");
        clientPair.hardwareClient.send("hardware vw 56 3");
        clientPair.hardwareClient.send("hardware vw 56 4");
        clientPair.hardwareClient.send("hardware vw 56 dddyyyiii");

        appClient.verifyResult(hardware(1, "1-0 vw 56 1"));
        appClient.verifyResult(hardware(2, "1-0 vw 56 2"));
        appClient.verifyResult(hardware(3, "1-0 vw 56 3"));
        appClient.verifyResult(hardware(4, "1-0 vw 56 4"));
        appClient.verifyResult(hardware(5, "1-0 vw 56 dddyyyiii"));

        appClient.sync(1, 0);
        appClient.verifyResult(ok(6));

        appClient.verifyResult(appSync("1-0 vm 56 1 2 3 4 dddyyyiii"));
    }

    @Test
    public void testTableSyncWorkForNewCommandFormat() throws Exception {
        clientPair.appClient.stop();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "2.26.0");
        appClient.verifyResult(ok(1));

        appClient.send("loadProfileGzipped");
        Profile profile = appClient.parseProfile(2);
        assertEquals(16, profile.dashBoards[0].widgets.length);

        appClient.send("getEnergy");
        appClient.verifyResult(produce(3, GET_ENERGY, "7500"));

        Table table = new Table();
        table.pin = 56;
        table.pinType = PinType.VIRTUAL;
        table.isClickableRows = true;
        table.isReoderingAllowed = true;
        table.height = 2;
        table.width = 2;

        appClient.createWidget(1, table);
        appClient.verifyResult(ok(4));

        clientPair.hardwareClient.send("hardware vw 56 add 0 Row1 1");
        clientPair.hardwareClient.send("hardware vw 56 add 1 Row2 2");
        clientPair.hardwareClient.send("hardware vw 56 add 2 Row3 3");
        clientPair.hardwareClient.send("hardware vw 56 add 3 Row4 4");
        clientPair.hardwareClient.send("hardware vw 56 add 4 Row5 dddyyyiii");
        appClient.verifyResult(produce(1, HARDWARE, b("1-0 vw 56 add 0 Row1 1")));
        appClient.verifyResult(produce(2, HARDWARE, b("1-0 vw 56 add 1 Row2 2")));
        appClient.verifyResult(produce(3, HARDWARE, b("1-0 vw 56 add 2 Row3 3")));
        appClient.verifyResult(produce(4, HARDWARE, b("1-0 vw 56 add 3 Row4 4")));
        appClient.verifyResult(produce(5, HARDWARE, b("1-0 vw 56 add 4 Row5 dddyyyiii")));

        appClient.activate(1);
        appClient.verifyResult(ok(5));

        appClient.verifyResult(appSync("1-0 vm 56 add 0 Row1 1 true add 1 Row2 2 true add 2 Row3 3 true add 3 Row4 4 true add 4 Row5 dddyyyiii true"));
    }

    @Test
    public void testLCDSendsSyncOnActivate() throws Exception {
        clientPair.hardwareClient.send("hardware vw 20 p 0 0 Hello");
        clientPair.hardwareClient.send("hardware vw 20 p 0 1 World");

        clientPair.appClient.verifyResult(produce(1, HARDWARE, b("1-0 vw 20 p 0 0 Hello")));
        clientPair.appClient.verifyResult(produce(2, HARDWARE, b("1-0 vw 20 p 0 1 World")));

        clientPair.appClient.sync(1);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 0 Hello"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 1 World"));
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

        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 20 p 0 0 H1"));
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 20 p 0 1 H2"));
        clientPair.appClient.verifyResult(hardware(3, "1-0 vw 20 p 0 2 H3"));
        clientPair.appClient.verifyResult(hardware(4, "1-0 vw 20 p 0 3 H4"));
        clientPair.appClient.verifyResult(hardware(5, "1-0 vw 20 p 0 4 H5"));
        clientPair.appClient.verifyResult(hardware(6, "1-0 vw 20 p 0 5 H6"));
        clientPair.appClient.verifyResult(hardware(7, "1-0 vw 20 p 0 6 H7"));

        clientPair.appClient.sync(1);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 1 H2"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 2 H3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 3 H4"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 4 H5"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 5 H6"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 20 p 0 6 H7"));
    }

    @Test
    public void testActivateAndGetSync() throws Exception {
        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(11)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
    }

    @Test
    //https://github.com/blynkkk/blynk-server/issues/443
    public void testSyncWidgetValueOverlapsWithPinStorage() throws Exception {
        clientPair.hardwareClient.send("hardware vw 125 1");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 125 1"));
        clientPair.appClient.reset();

        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 125 1"));


        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":0, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":125}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.reset();

        clientPair.hardwareClient.send("hardware vw 125 2");
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 125 2"));
        clientPair.appClient.reset();

        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 125 2"));
    }

    @Test
    public void testActivateAndGetSyncForSpecificDeviceId() throws Exception {
        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(11)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
    }

    @Test
    public void testSyncForDeviceSelectorAndSetProperty() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.ESP8266);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"deviceIds\":[0], \"width\":1, \"height\":1, \"value\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"height\":1, \"deviceId\":200000, \"x\":0, \"y\":0, \"label\":\"Button\", \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.setProperty(88, "label", "newLabel");
        clientPair.hardwareClient.setProperty(88, "label", "newLabel2");
        clientPair.appClient.verifyResult(setProperty(1, "1-0 88 label newLabel"));
        clientPair.appClient.verifyResult(setProperty(2, "1-0 88 label newLabel2"));

        clientPair.appClient.reset();

        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
        clientPair.appClient.verifyResult(setProperty(1111, "1-0 88 label newLabel2"));
        clientPair.appClient.never(setProperty(1111, "1-0 88 label newLabel"));
    }

    @Test
    public void testSyncForDeviceTilesAndSetProperty() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.ESP8266);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;
        deviceTiles.color = -231;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(2));

        PageTileTemplate tileTemplate = new PageTileTemplate(
                1,
                null,
                new int[] {0, device.id},
                "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, -75056000, -231, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(3));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 2322;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 1;
        valueDisplay.pinType = PinType.VIRTUAL;

        clientPair.appClient.createWidget(1, widgetId, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.hardwareClient.setProperty(1, "label", "newLabel");
        clientPair.appClient.verifyResult(setProperty(1, "1-0 1 label newLabel"));
        clientPair.appClient.reset();

        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
        clientPair.appClient.verifyResult(setProperty(1111, "1-0 1 label newLabel"));
    }

    @Test
    public void testSyncForDeviceSelectorAndSetPropertyAndMultiValueWidget() throws Exception {
        Device device0 = new Device(0, "My Dashboard", BoardType.ESP8266);
        device0.status = Status.ONLINE;
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"id\":200000, \"deviceIds\":[0], \"width\":1, \"height\":1, \"value\":1, \"x\":0, \"y\":0, \"label\":\"Some Text\", \"type\":\"DEVICE_SELECTOR\"}");
        clientPair.appClient.createWidget(1, "{\"id\":88, \"width\":1, \"deviceId\":200000, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":88}");
        clientPair.appClient.verifyResult(ok(2));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.setProperty(88, "label", "newLabel");
        clientPair.hardwareClient.setProperty(88, "label", "newLabel2");
        clientPair.appClient.verifyResult(setProperty(1, "1-0 88 label newLabel"));
        clientPair.appClient.verifyResult(setProperty(2, "1-0 88 label newLabel2"));

        clientPair.appClient.reset();

        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
        clientPair.appClient.verifyResult(setProperty(1111, "1-0 88 label newLabel2"));
        clientPair.appClient.never(setProperty(1111, "1-0 88 label newLabel"));
    }

    @Test
    public void testActivateAndGetSyncForTimeInput() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"TIME_INPUT\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":1,\"height\":1}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw " + "99 82800 82860 Europe/Kiev 1");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), eq(hardware(2, "vw 99 82800 82860 Europe/Kiev 1")));

        clientPair.appClient.reset();

        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-0 dw 1 1"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 2 1"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 3 0"));
        clientPair.appClient.verifyResult(appSync("1-0 dw 5 1"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 4 244"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 7 3"));
        clientPair.appClient.verifyResult(appSync("1-0 aw 30 3"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 0 89.888037459418"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 11 -58.74774244674501"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 13 60 143 158"));
        clientPair.appClient.verifyResult(appSync("1-0 vw 99 82800 82860 Europe/Kiev 1"));
    }

    @Test
    public void testActivateAndGetSyncForNonExistingDeviceId() throws Exception {
        clientPair.appClient.sync(1, 1);

        verify(clientPair.appClient.responseMock, timeout(500).times(1)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));
    }

    @Test
    public void testLCDOnActivateSendsCorrectBodySimpleModeAndAnotherDevice() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.createWidget(1, "{\"deviceId\":1,\"type\":\"LCD\",\"id\":1923810267,\"x\":0,\"y\":6,\"color\":600084223,\"width\":8,\"height\":2,\"tabId\":0,\"" +
                "pins\":[" +
                "{\"pin\":10,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"10\"}," +
                "{\"pin\":11,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"11\"}]," +
                "\"advancedMode\":false,\"textLight\":false,\"textLightOn\":false,\"frequency\":1000}");

        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 1);

        verify(clientPair.appClient.responseMock, timeout(500).times(3)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync("1-1 vw 10 10"));
        clientPair.appClient.verifyResult(appSync("1-1 vw 11 11"));
    }

}
