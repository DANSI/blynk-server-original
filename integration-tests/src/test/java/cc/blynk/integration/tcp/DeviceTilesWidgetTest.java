package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
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
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTile;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileMode;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.AppSyncMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.hardware.HardwareServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.server.core.model.serialization.JsonParser.MAPPER;
import static cc.blynk.server.core.protocol.enums.Response.NO_DATA;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
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
        hardwareServer = new HardwareServer(holder).start();
        appServer = new AppServer(holder).start();

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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3));
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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4));
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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TileTemplate tileTemplate = new TileTemplate(0, null, null, "123",
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + "{\"alignment\":\"LEFT\",\"color\":600084223,\"deviceIds\":[0],\"disableWhenOffline\":false," +
                "\"id\":0,\"mode\":\"PAGE\",\"name\":\"Template 1\"," +
                "\"pin\":{\"max\":255,\"min\":0,\"pin\":5,\"pinType\":\"VIRTUAL\",\"pwmMode\":false," +
                "\"rangeMappingOn\":false},\"showDeviceName\":true,\"valueName\":\"Temperature\"," +
                "\"valueSuffix\":\"%\",\"widgets\":[]}}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4));
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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);
        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, dataStream, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4));
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
                TileMode.PAGE, dataStream, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6));
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

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(1, device)));

        clientPair.appClient.reset();

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);
        tileTemplate = new TileTemplate(1, null, new int[] {0, 1}, "123",
                TileMode.PAGE, dataStream, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4));
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(2, deviceTiles.tiles.length);

        int deviceIdIndex = 0;
        for (DeviceTile deviceTile : deviceTiles.tiles) {
            assertEquals(deviceIdIndex++, deviceTile.deviceId);
            assertEquals(tileTemplate.id, deviceTile.templateId);
            assertNotNull(deviceTile.dataStream);
            assertEquals(1, deviceTile.dataStream.pin);
            assertEquals(PinType.VIRTUAL, deviceTile.dataStream.pinType);
        }

        dataStream = new DataStream((byte) 2, PinType.VIRTUAL);
        tileTemplate = new TileTemplate(1, null, new int[] {0, 1}, "123",
                TileMode.PAGE, dataStream, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(5)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6));
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(2, deviceTiles.tiles.length);

        deviceIdIndex = 0;
        for (DeviceTile deviceTile : deviceTiles.tiles) {
            assertEquals(deviceIdIndex++, deviceTile.deviceId);
            assertEquals(tileTemplate.id, deviceTile.templateId);
            assertNotNull(deviceTile.dataStream);
            assertEquals(2, deviceTile.dataStream.pin);
            assertEquals(PinType.VIRTUAL, deviceTile.dataStream.pinType);
        }
    }

    @Test
    public void syncForSpecificDeviceTile() throws Exception {
        Device device1 = new Device(1, "My Device", "ESP8266");
        device1.status = Status.OFFLINE;

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody();
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(1, device)));

        clientPair.appClient.reset();

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TileTemplate tileTemplate = new TileTemplate(1, null, null, "123",
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        DataStream dataStream = new DataStream((byte) 5, PinType.VIRTUAL);

        Button button = new Button();
        button.width = 2;
        button.height = 2;
        button.pin = 2;
        button.pinType = PinType.VIRTUAL;

        ValueDisplay valueDisplay = new ValueDisplay();
        button.width = 2;
        button.height = 2;
        button.pin = 1;
        button.pinType = PinType.VIRTUAL;


        tileTemplate = new TileTemplate(1, new Widget[]{button, valueDisplay}, new int[] {0, 1}, "123",
                TileMode.PAGE, dataStream, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4));
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals(2, deviceTiles.templates[0].widgets.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("123", deviceTiles.templates[0].name);
        assertEquals(2, deviceTiles.tiles.length);

        int deviceIdIndex = 0;
        for (DeviceTile deviceTile : deviceTiles.tiles) {
            assertEquals(deviceIdIndex++, deviceTile.deviceId);
            assertEquals(tileTemplate.id, deviceTile.templateId);
            assertNotNull(deviceTile.dataStream);
            assertEquals(5, deviceTile.dataStream.pin);
            assertEquals(PinType.VIRTUAL, deviceTile.dataStream.pinType);
        }


        clientPair.hardwareClient.send("hardware vw 5 101");
        clientPair.hardwareClient.send("hardware vw 6 102");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("1 vw 5 101"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(2, b("1 vw 6 102"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody());
        assertNotNull(deviceTiles);

        DeviceTile deviceTile = deviceTiles.tiles[0];
        assertEquals(0, deviceTile.deviceId);
        assertNotNull(deviceTile.dataStream);
        assertEquals(5, deviceTile.dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTile.dataStream.pinType);
        assertEquals("101", deviceTile.dataStream.value);

        DeviceTile deviceTile2 = deviceTiles.tiles[1];
        assertEquals(1, deviceTile2.deviceId);
        assertNotNull(deviceTile2.dataStream);
        assertEquals(5, deviceTile2.dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTile2.dataStream.pinType);
        assertNull(deviceTile2.dataStream.value);

        clientPair.appClient.reset();
        clientPair.appClient.send("appSync 1-0");

        verify(clientPair.appClient.responseMock, timeout(500).times(13)).channelRead(any(), any());

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 dw 1 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 dw 2 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 aw 3 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 dw 5 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 4 244"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 aw 7 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 aw 30 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 0 89.888037459418"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 1 -58.74774244674501"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(b("1 vw 13 60 143 158"))));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(1111, b("1 vw 5 101"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new AppSyncMessage(1111, b("1 vw 6 102"))));
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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        int[] deviceIds = new int[] {0};
        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);

        TileTemplate tileTemplate = new TileTemplate(1, null, deviceIds, "123",
                TileMode.PAGE, dataStream, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3));
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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        int[] deviceIds = new int[] {0};
        DataStream dataStream = new DataStream((byte) 1, PinType.VIRTUAL);

        TileTemplate tileTemplate = new TileTemplate(1, null, deviceIds, "123",
                TileMode.PAGE, dataStream, null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3));
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

        clientPair.appClient.send("updateWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(5));
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
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(4)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        DeviceTiles deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(5));
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
                TileMode.PAGE, null, null, null, 0, TextAlignment.LEFT, false, false);
        deviceTiles.templates = new TileTemplate[] {
                tileTemplate
        };

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(2));
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("123", deviceTiles.templates[0].name);


        clientPair.appClient.send("deleteTemplate " + b("1 " + widgetId + " " + tileTemplate.id));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4));
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
                TileMode.PAGE, new DataStream((byte) -1, null), null, null, 0, TextAlignment.LEFT, false, false);
        deviceTiles.templates = new TileTemplate[] {
                tileTemplate
        };

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        tileTemplate = new TileTemplate(1, null, new int[] {0}, "123",
                TileMode.PAGE, new DataStream((byte) 1, PinType.VIRTUAL), null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("updateTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3));
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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

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
                deviceIds, "123", TileMode.PAGE, new DataStream((byte) 1, PinType.VIRTUAL), null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 DAY"));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NO_DATA)));
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

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

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
                deviceIds, "123", TileMode.PAGE, new DataStream((byte) 1, PinType.VIRTUAL), null, null, 0, TextAlignment.LEFT, false, false);

        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + MAPPER.writeValueAsString(tileTemplate));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getenhanceddata 1-0" + b(" 432 DAY"));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, NO_DATA)));
    }

}
