package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.outputs.TextAlignment;
import cc.blynk.server.core.model.widgets.outputs.ValueDisplay;
import cc.blynk.server.core.model.widgets.outputs.graph.AggregationFunctionType;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphType;
import cc.blynk.server.core.model.widgets.ui.Menu;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.Tile;
import cc.blynk.server.core.model.widgets.ui.tiles.TileMode;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.model.serialization.JsonParser.MAPPER;
import static cc.blynk.server.core.model.widgets.FrequencyWidget.READING_MSG_ID;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Response.NO_DATA;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertArrayEquals;
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
 * Created on 7/09/2016.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DeviceTilesWidgetTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        appServer = new AppAndHttpsServer(holder).start();

        if (clientPair == null) {
            clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
        }
        clientPair.hardwareClient.reset();
        clientPair.appClient.reset();
    }

    @After
    public void shutdown() {
        appServer.close();
        hardwareServer.close();
        clientPair.stop();
    }

    @Test
    public void createTemplate() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(0, deviceTiles.tiles.length);
    }

    @Test
    public void createTemplateAndUpdate() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(3));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNull(deviceTiles.tiles[0].dataStream);
    }

    @Test
    public void createTemplateAndUpdate2() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(0, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + "{\"alignment\":\"LEFT\",\"color\":600084223,\"deviceIds\":[0],\"disableWhenOffline\":false," +
                "\"id\":0,\"mode\":\"PAGE\",\"name\":\"Template 1\"," +
                "\"pin\":{\"max\":255,\"min\":0,\"pin\":5,\"pinType\":\"VIRTUAL\",\"pwmMode\":false," +
                "\"rangeMappingOn\":false},\"showDeviceName\":true,\"valueName\":\"Temperature\"," +
                "\"valueSuffix\":\"%\",\"widgets\":[]}}");
        clientPair.appClient.verifyResult(ok(3));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("Template 1", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(5, deviceTiles.tiles[0].dataStream.pin);
    }

    @Test
    public void createTemplateAndUpdatePin() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);
        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(1, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);

        dataStream = new DataStream((byte) 2, PinType.VIRTUAL);
        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(2, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);
    }

    @Test
    public void createTemplateAndUpdatePinFor2Templates() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.getDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.reset();

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);
        tileTemplate = new TileTemplate(1, null, new int[] {0, 1}, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(2, deviceTiles.tiles.length);

        int deviceIdIndex = 0;
        for (Tile tile : deviceTiles.tiles) {
            assertEquals(deviceIdIndex++, tile.deviceId);
            assertEquals(tileTemplate.id, tile.templateId);
            assertNotNull(tile.dataStream);
            assertEquals(1, tile.dataStream.pin);
            assertEquals(PinType.VIRTUAL, tile.dataStream.pinType);
        }

        dataStream = new DataStream((byte) 2, PinType.VIRTUAL);
        tileTemplate = new TileTemplate(1, null, new int[] {0, 1}, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(2, deviceTiles.tiles.length);

        deviceIdIndex = 0;
        for (Tile tile : deviceTiles.tiles) {
            assertEquals(deviceIdIndex++, tile.deviceId);
            assertEquals(tileTemplate.id, tile.templateId);
            assertNotNull(tile.dataStream);
            assertEquals(2, tile.dataStream.pin);
            assertEquals(PinType.VIRTUAL, tile.dataStream.pinType);
        }
    }

    @Test
    public void syncForSpecificDeviceTile() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.getDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.reset();

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((byte) 5, PinType.VIRTUAL);

        Button button = new Button();
        button.width = 2;
        button.height = 2;
        button.pin = 2;
        button.pinType = PinType.VIRTUAL;
        button.deviceId = 0;

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.deviceId = 0;

        tileTemplate = new TileTemplate(1, new Widget[]{button, valueDisplay}, new int[] {0, 1}, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals(2, deviceTiles.templates[0].widgets.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(2, deviceTiles.tiles.length);

        int deviceIdIndex = 0;
        for (Tile tile : deviceTiles.tiles) {
            assertEquals(deviceIdIndex++, tile.deviceId);
            assertEquals(tileTemplate.id, tile.templateId);
            assertNotNull(tile.dataStream);
            assertEquals(5, tile.dataStream.pin);
            assertEquals(PinType.VIRTUAL, tile.dataStream.pinType);
        }


        clientPair.hardwareClient.send("hardware vw 5 101");
        clientPair.hardwareClient.send("hardware vw 6 102");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 5 101"));
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 6 102"));

        clientPair.appClient.reset();
        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(), 0);
        assertNotNull(deviceTiles);

        Tile tile = deviceTiles.tiles[0];
        assertEquals(0, tile.deviceId);
        assertNotNull(tile.dataStream);
        assertEquals(5, tile.dataStream.pin);
        assertEquals(PinType.VIRTUAL, tile.dataStream.pinType);
        assertEquals("101", tile.dataStream.value);

        Tile tile2 = deviceTiles.tiles[1];
        assertEquals(1, tile2.deviceId);
        assertNotNull(tile2.dataStream);
        assertEquals(5, tile2.dataStream.pin);
        assertEquals(PinType.VIRTUAL, tile2.dataStream.pinType);
        assertNull(tile2.dataStream.value);

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(13)).channelRead(any(), any());

        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.verifyResult(appSync(b("1-0 dw 1 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 dw 2 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 3 0")));
        clientPair.appClient.verifyResult(appSync(b("1-0 dw 5 1")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 4 244")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 7 3")));
        clientPair.appClient.verifyResult(appSync(b("1-0 aw 30 3")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 0 89.888037459418")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 1 -58.74774244674501")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 13 60 143 158")));

        clientPair.appClient.verifyResult(appSync(1111, b("1-0 vw 5 101")));
        clientPair.appClient.verifyResult(appSync(1111, b("1-0 vw 6 102")));
    }

    @Test
    public void readingWidgetWorksForDeviceTiles() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.getDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.reset();

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((byte) 5, PinType.VIRTUAL);

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 1000;
        valueDisplay.deviceId = 0;

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(holder.readingWidgetsWorker, 0, 1000, TimeUnit.MILLISECONDS);

        tileTemplate = new TileTemplate(1, new Widget[]{valueDisplay}, new int[] {0}, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);

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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 1 -58.74774244674501")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 13 60 143 158")));

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(READING_MSG_ID, HARDWARE, b("vr 77"))));
    }

    @Test
    public void readingWidgetWorksForDeviceTiles2() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.getDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();
        hardClient2.login(device.token);
        hardClient2.verifyResult(ok(1));

        clientPair.appClient.reset();

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((byte) 5, PinType.VIRTUAL);

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 1000;
        valueDisplay.deviceId = 1;

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(holder.readingWidgetsWorker, 0, 1000, TimeUnit.MILLISECONDS);

        tileTemplate = new TileTemplate(1, new Widget[]{valueDisplay}, new int[] {1}, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 1);

        clientPair.appClient.verifyAny();
        clientPair.appClient.verifyResult(ok(1));

        verify(hardClient2.responseMock, timeout(2000)).channelRead(any(), eq(produce(READING_MSG_ID, HARDWARE, b("vr 77"))));
        verify(clientPair.hardwareClient.responseMock, never()).channelRead(any(), eq(produce(READING_MSG_ID, HARDWARE, b("vr 77"))));
    }

    @Test
    public void createTemplateWithTiles() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        int[] deviceIds = new int[] {0};
        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);

        TileTemplate tileTemplate = new TileTemplate(1, null, deviceIds, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals(1, deviceTiles.templates[0].deviceIds.length);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(1, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);
    }

    @Test
    public void checkDeviceTilesWidgetSettingsUpdatedWithoutTemplateAndTilefields() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        int[] deviceIds = new int[] {0};
        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);

        TileTemplate tileTemplate = new TileTemplate(1, null, deviceIds, "123",
                TileMode.PAGE, "ESP8266", dataStream, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals(1, deviceTiles.templates[0].deviceIds.length);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(1, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);

        deviceTiles.templates = null;
        deviceTiles.tiles = null;

        clientPair.appClient.updateWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(5), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals(1, deviceTiles.templates[0].deviceIds.length);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(1, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);
    }

    @Test
    public void createTemplateWithTilesAndDelete() throws Exception {
        long widgetId = 21321;
        int templateId = 1;
        createTemplateWithTiles();

        clientPair.appClient.send("deleteTemplate " + b("1 " + widgetId + " " + templateId));
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        DeviceTiles deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(5), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(0, deviceTiles.templates.length);
        assertEquals(0, deviceTiles.tiles.length);
    }

    @Test
    public void deleteTemplate() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);
        deviceTiles.templates = new TileTemplate[] {
                tileTemplate
        };

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(2), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("123", deviceTiles.templates[0].name);


        clientPair.appClient.send("deleteTemplate " + b("1 " + widgetId + " " + tileTemplate.id));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(0, deviceTiles.templates.length);
    }

    @Test
    public void updateTemplateCreateWithWidget() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", new DataStream((byte) -1, null), null, null, 0, TextAlignment.LEFT, false, false, null, null);
        deviceTiles.templates = new TileTemplate[] {
                tileTemplate
        };

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, "ESP8266", new DataStream((byte) 1, PinType.VIRTUAL), null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(1, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);
    }

    @Test
    public void getEnhancedHistoryGraphWorksForTiles() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        int[] deviceIds = new int[] {0};

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        GraphDataStream graphDataStream = new GraphDataStream(
                null, GraphType.LINE, 0, 100_000,
                new DataStream((byte) 88, PinType.VIRTUAL),
                AggregationFunctionType.MAX, 0, null, null, null, 0, 0, false, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        TileTemplate tileTemplate = new TileTemplate(1,
                new Widget[] {
                        enhancedHistoryGraph
                },
                deviceIds, "123", TileMode.PAGE, "ESP8266", new DataStream((byte) 1, PinType.VIRTUAL), null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 DAY"));
        clientPair.appClient.verifyResult(new ResponseMessage(3, NO_DATA));
    }

    @Test
    public void getEnhancedHistoryGraphWorksForTiles2() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        int[] deviceIds = new int[] {0};

        EnhancedHistoryGraph enhancedHistoryGraph = new EnhancedHistoryGraph();
        enhancedHistoryGraph.id = 432;
        enhancedHistoryGraph.width = 8;
        enhancedHistoryGraph.height = 4;
        GraphDataStream graphDataStream = new GraphDataStream(
                null, GraphType.LINE, 0, 100_000,
                new DataStream((byte) 88, PinType.VIRTUAL),
                AggregationFunctionType.MAX, 0, null, null, null, 0, 0, false, null, false, false, false);
        enhancedHistoryGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        TileTemplate tileTemplate = new TileTemplate(1,
                new Widget[] {
                        enhancedHistoryGraph
                },
                deviceIds, "123", TileMode.PAGE, "ESP8266", new DataStream((byte) 1, PinType.VIRTUAL), null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getenhanceddata 1-0" + b(" 432 DAY"));
        clientPair.appClient.verifyResult(new ResponseMessage(3, NO_DATA));
    }

    @Test
    public void energyCalculationsAreCorrectWhenAddingRemovingWidgets() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "2600")));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.createWidget(1, b("21321 1 ") + "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, GET_ENERGY, "2400")));

        clientPair.appClient.createWidget(1, b("21321 1 ") + "{\"id\":101, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":3}");
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7, GET_ENERGY, "2200")));

        clientPair.appClient.deleteWidget(1, 101);
        clientPair.appClient.verifyResult(ok(8));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(9, GET_ENERGY, "2400")));

        clientPair.appClient.deleteWidget(1, 21321);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(10)));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(11, GET_ENERGY, "7500")));
    }

    @Test
    public void updateCommandWorksForWidgetWithinDeviceTiles() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.createWidget(1, b("21321 1 ") + "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.createWidget(1, b("21321 1 ") + "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(4)));

        clientPair.appClient.updateWidget(1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 3\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":3}");
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("123", deviceTiles.templates[0].name);
        assertNotNull(deviceTiles.templates[0].widgets[0]);
        assertEquals("Some Text 3", deviceTiles.templates[0].widgets[0].label);
    }

    @Test
    public void testHugeWidgetIsCreatedWithinDeviceTiles() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, "ESP8266", null, null, null, 0, TextAlignment.LEFT, false, false, null, null);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        clientPair.appClient.verifyResult(ok(2));

        Menu menu = new Menu();
        menu.id = 172652;
        menu.x = 2;
        menu.y = 34;
        menu.color = 600084223;
        menu.width = 6;
        menu.height = 1;
        menu.label = "Set Volume";
        menu.deviceId = 252521;
        menu.labels = new String[] {"Item1", "Item2"};

        clientPair.appClient.createWidget(1, b("21321 1 ") + JsonParser.MAPPER.writeValueAsString(menu));
        clientPair.appClient.verifyResult(ok(3));

        List<String> list = new ArrayList<>();
        for (float i = 0; i < 49.99; i += 0.1F) {
            list.add(String.format("%.2f", i));
        }
        menu.labels = list.toArray(new String[0]);

        clientPair.appClient.updateWidget(1, JsonParser.MAPPER.writeValueAsString(menu));
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(5), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("123", deviceTiles.templates[0].name);
        assertNotNull(deviceTiles.templates[0].widgets[0]);
        assertEquals(500, ((Menu) deviceTiles.templates[0].widgets[0]).labels.length);
    }

}
