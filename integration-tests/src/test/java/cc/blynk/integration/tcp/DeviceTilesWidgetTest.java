package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.NumberInput;
import cc.blynk.server.core.model.widgets.controls.Terminal;
import cc.blynk.server.core.model.widgets.outputs.ValueDisplay;
import cc.blynk.server.core.model.widgets.outputs.graph.AggregationFunctionType;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphType;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.Menu;
import cc.blynk.server.core.model.widgets.ui.Tab;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.Tile;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.model.widgets.ui.tiles.templates.ButtonTileTemplate;
import cc.blynk.server.core.model.widgets.ui.tiles.templates.PageTileTemplate;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.utils.FileUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static cc.blynk.integration.TestUtil.appSync;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.sleep;
import static cc.blynk.server.core.model.widgets.FrequencyWidget.READING_MSG_ID;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Response.NO_DATA;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
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
public class DeviceTilesWidgetTest extends SingleServerInstancePerTest {

    @Test
    public void createPageTemplate() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;
        deviceTiles.color = -231;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        PageTileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, -75056000, -231, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertEquals(-231, deviceTiles.color);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("name", deviceTiles.templates[0].name);
        assertTrue(deviceTiles.templates[0] instanceof PageTileTemplate);
        PageTileTemplate pageTileTemplate = (PageTileTemplate) deviceTiles.templates[0];
        assertEquals(0, deviceTiles.tiles.length);
        assertEquals(-75056000, pageTileTemplate.color);
        assertEquals(-231, pageTileTemplate.tileColor);
    }

    @Test
    public void createDeviceTilesAndEditColors() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;
        deviceTiles.color = 0;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        PageTileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        deviceTiles.color = -231;

        clientPair.appClient.updateWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(3));

        tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, -1, -231, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(5), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertEquals(-231, deviceTiles.color);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("name", deviceTiles.templates[0].name);
        assertTrue(deviceTiles.templates[0] instanceof PageTileTemplate);
        PageTileTemplate pageTileTemplate = (PageTileTemplate) deviceTiles.templates[0];
        assertEquals(0, deviceTiles.tiles.length);
        assertEquals(-1, pageTileTemplate.color);
        assertEquals(-231, pageTileTemplate.tileColor);
    }

    @Test
    public void createPageTemplateWithOutModeField() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.createTemplate(1, widgetId, "{\"id\":1,\"templateId\":\"123\",\"name\":\"name\",\"iconName\":\"iconName\",\"boardType\":\"ESP8266\",\"showDeviceName\":false,\"color\":0,\"tileColor\":0,\"fontSize\":\"LARGE\",\"showTileLabel\":false,\"pin\":{\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"VIRTUAL\",\"min\":0.0,\"max\":255.0}}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("name", deviceTiles.templates[0].name);
        assertTrue(deviceTiles.templates[0] instanceof PageTileTemplate);
        assertEquals(0, deviceTiles.tiles.length);
    }

    @Test
    public void createButtonTileTemplate() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        ButtonTileTemplate tileTemplate = new ButtonTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, false, false, null, null);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("name", deviceTiles.templates[0].name);
        assertTrue(deviceTiles.templates[0] instanceof ButtonTileTemplate);
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

        PageTileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(3));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("name", deviceTiles.templates[0].name);
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

        PageTileTemplate tileTemplate = new PageTileTemplate(0,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
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

        PageTileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((short) 1, PinType.VIRTUAL);
        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("name", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(1, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);

        dataStream = new DataStream((short) 2, PinType.VIRTUAL);
        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("name", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(2, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);
    }

    @Test
    public void createTemplateAndUpdatePinFor2Templates() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
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

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((short) 1, PinType.VIRTUAL);
        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0, 1}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(4), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("name", deviceTiles.templates[0].name);
        assertEquals(2, deviceTiles.tiles.length);

        int deviceIdIndex = 0;
        for (Tile tile : deviceTiles.tiles) {
            assertEquals(deviceIdIndex++, tile.deviceId);
            assertEquals(tileTemplate.id, tile.templateId);
            assertNotNull(tile.dataStream);
            assertEquals(1, tile.dataStream.pin);
            assertEquals(PinType.VIRTUAL, tile.dataStream.pinType);
        }

        dataStream = new DataStream((short) 2, PinType.VIRTUAL);
        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0, 1}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("name", deviceTiles.templates[0].name);
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
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
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

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((short) 5, PinType.VIRTUAL);

        Button button = new Button();
        button.id = 2321;
        button.width = 2;
        button.height = 2;
        button.pin = 2;
        button.pinType = PinType.VIRTUAL;

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 2322;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;

        clientPair.appClient.createWidget(1, widgetId, tileTemplate.id, button);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.createWidget(1, widgetId, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(4));

        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0, 1}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals(2, deviceTiles.templates[0].widgets.length);
        assertArrayEquals(new int[] {0, 1}, deviceTiles.templates[0].deviceIds);
        assertEquals("name", deviceTiles.templates[0].name);
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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 11 -58.74774244674501")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 13 60 143 158")));

        clientPair.appClient.verifyResult(appSync(1111, b("1-0 vw 5 101")));
        clientPair.appClient.verifyResult(appSync(1111, b("1-0 vw 6 102")));
    }

    @Test
    public void readingWidgetWorksForDeviceTiles() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.reset();

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((short) 5, PinType.VIRTUAL);

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 1234;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 1000;
        valueDisplay.deviceId = -1;

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(holder.readingWidgetsWorker, 0, 1000, TimeUnit.MILLISECONDS);

        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(4));

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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 11 -58.74774244674501")));
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 13 60 143 158")));

        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(READING_MSG_ID, HARDWARE, b("vr 77"))));
    }

    @Test
    public void readingWidgetWorksForAllTilesWithinDeviceTiles() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

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

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 1234;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 1000;
        valueDisplay.deviceId = -1;

        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0, 1}, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, device1.id);
        clientPair.appClient.verifyResult(ok(1));

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(holder.readingWidgetsWorker, 0, 1000, TimeUnit.MILLISECONDS);

        hardClient2.verifyResult(produce(READING_MSG_ID, HARDWARE, b("vr 77")));
        clientPair.hardwareClient.verifyResult(produce(READING_MSG_ID, HARDWARE, b("vr 77")));
    }

    @Test
    public void doNotPerformReadCommandWhenNoReadingWidgetInsideTileTemplate() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

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

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 1234;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 0;
        valueDisplay.deviceId = -1;

        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0, 1}, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, device1.id);
        clientPair.appClient.verifyResult(ok(1));

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(holder.readingWidgetsWorker, 0, 1000, TimeUnit.MILLISECONDS);

        sleep(1200);

        hardClient2.never(produce(READING_MSG_ID, HARDWARE, b("vr 77")));
        clientPair.hardwareClient.never(produce(READING_MSG_ID, HARDWARE, b("vr 77")));
    }

    @Test
    public void deviceRemovalDoesntEraseAllTiles() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

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

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        DataStream dataStream = new DataStream((short) 66, PinType.VIRTUAL);
        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 1234;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 0;
        valueDisplay.deviceId = -1;

        tileTemplate = new PageTileTemplate(1,
                null, new int[]{0, 1}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, device1.id);
        clientPair.appClient.verifyResult(ok(1));

        hardClient2.send("hardware vw 66 444");
        clientPair.appClient.verifyResult(hardware(2, "1-1 vw 66 444"));

        clientPair.hardwareClient.send("hardware vw 66 555");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 66 555"));

        clientPair.appClient.deleteDevice(1, 1);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(appSync("1-0 vw 66 555"));
    }

    @Test
    public void addingNewDeviceToTheTilesPreservesTileValue() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

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

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        DataStream dataStream = new DataStream((short) 66, PinType.VIRTUAL);
        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 1234;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 0;
        valueDisplay.deviceId = -1;

        tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.hardwareClient.send("hardware vw 66 444");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 66 444"));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(appSync("1-0 vw 66 444"));

        tileTemplate = new PageTileTemplate(1,
                null, new int[] {0, 1}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);
        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(appSync("1-0 vw 66 444"));
    }

    @Test
    public void addingNewDeviceToTheTilesPreservesTemplateValue() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

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

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        DataStream dataStream = new DataStream((short) 66, PinType.VIRTUAL);
        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 1234;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 77;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.frequency = 0;
        valueDisplay.deviceId = -1;

        tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(4));

        clientPair.hardwareClient.send("hardware vw 77 444");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 77 444"));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(appSync("1-0 vw 77 444"));

        tileTemplate = new PageTileTemplate(1,
                null, new int[] {0, 1}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);
        clientPair.appClient.updateTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(ok(1));
        clientPair.appClient.verifyResult(appSync("1-0 vw 77 444"));
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
        DataStream dataStream = new DataStream((short) 1, PinType.VIRTUAL);

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, deviceIds, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals(1, deviceTiles.templates[0].deviceIds.length);
        assertEquals("name", deviceTiles.templates[0].name);
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
        DataStream dataStream = new DataStream((short) 1, PinType.VIRTUAL);

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, deviceIds, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals(1, deviceTiles.templates[0].deviceIds.length);
        assertEquals("name", deviceTiles.templates[0].name);
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
        assertEquals("name", deviceTiles.templates[0].name);
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

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);
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
        assertEquals("name", deviceTiles.templates[0].name);


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

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) -1, null),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);
        deviceTiles.templates = new TileTemplate[] {
                tileTemplate
        };

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));


        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(3), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertArrayEquals(new int[] {0}, deviceTiles.templates[0].deviceIds);
        assertEquals("name", deviceTiles.templates[0].name);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals(0, deviceTiles.tiles[0].deviceId);
        assertEquals(tileTemplate.id, deviceTiles.tiles[0].templateId);
        assertNotNull(deviceTiles.tiles[0].dataStream);
        assertEquals(1, deviceTiles.tiles[0].dataStream.pin);
        assertEquals(PinType.VIRTUAL, deviceTiles.tiles[0].dataStream.pinType);
    }

    @Test
    public void getSuperchartGraphWorksForTiles() throws Exception {
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

        Superchart SuperchartGraph = new Superchart();
        SuperchartGraph.id = 432;
        SuperchartGraph.width = 8;
        SuperchartGraph.height = 4;
        GraphDataStream graphDataStream = new GraphDataStream(
                null, GraphType.LINE, 0, 100_000,
                new DataStream((short) 88, PinType.VIRTUAL),
                AggregationFunctionType.MAX, 0, null, null, null, 0, 0, false, null, false, false, false, null, 0, false, 0);
        SuperchartGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        TileTemplate tileTemplate = new PageTileTemplate(1,
                new Widget[]{SuperchartGraph}, deviceIds, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getenhanceddata 1" + b(" 432 DAY"));
        clientPair.appClient.verifyResult(new ResponseMessage(3, NO_DATA));
    }

    @Test
    public void getSuperchartGraphWorksForTiles2() throws Exception {
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

        Superchart SuperchartGraph = new Superchart();
        SuperchartGraph.id = 432;
        SuperchartGraph.width = 8;
        SuperchartGraph.height = 4;
        GraphDataStream graphDataStream = new GraphDataStream(
                null, GraphType.LINE, 0, 100_000,
                new DataStream((short) 88, PinType.VIRTUAL),
                AggregationFunctionType.MAX, 0, null, null, null, 0, 0, false, null, false, false, false, null, 0, false, 0);
        SuperchartGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        TileTemplate tileTemplate = new PageTileTemplate(1,
                new Widget[]{SuperchartGraph}, deviceIds, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short)1, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getenhanceddata 1-0" + b(" 432 DAY"));
        clientPair.appClient.verifyResult(new ResponseMessage(3, NO_DATA));
    }

    @Test
    public void exportSuperchartGraphWorksForTiles() throws Exception {
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

        Superchart SuperchartGraph = new Superchart();
        SuperchartGraph.id = 432;
        SuperchartGraph.width = 8;
        SuperchartGraph.height = 4;
        GraphDataStream graphDataStream = new GraphDataStream(
                null, GraphType.LINE, 0, 0,
                new DataStream((short) 88, PinType.VIRTUAL),
                AggregationFunctionType.MAX, 0, null, null, null, 0, 0, false, null, false, false, false, null, 0, false, 0);
        SuperchartGraph.dataStreams = new GraphDataStream[] {
                graphDataStream
        };

        TileTemplate tileTemplate = new PageTileTemplate(1,
                new Widget[]{SuperchartGraph}, deviceIds, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("export 1 432");
        clientPair.appClient.verifyResult(new ResponseMessage(3, NO_DATA));

        Path userReportDirectory = Paths.get(holder.props.getProperty("data.folder"), "data", getUserName());
        Files.createDirectories(userReportDirectory);
        Path userReportFile = Paths.get(userReportDirectory.toString(),
                ReportingDiskDao.generateFilename(1, 0, PinType.VIRTUAL, (short) 88, GraphGranularityType.MINUTE));
        FileUtils.write(userReportFile, 1.1, 1L);
        FileUtils.write(userReportFile, 2.2, 2L);

        clientPair.appClient.send("export 1 432");
        clientPair.appClient.verifyResult(ok(4));
        verify(holder.mailWrapper, timeout(1000)).sendHtml(eq(getUserName()), eq("History graph data for project My Dashboard"), contains("/" + getUserName() + "_1_0_v88_"));

        clientPair.appClient.send("deleteEnhancedData 1\0" + "432");
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("export 1 432");
        clientPair.appClient.verifyResult(new ResponseMessage(6, NO_DATA));
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
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "5800")));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.createWidget(1, 21321, 1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        clientPair.appClient.verifyResult(ok(4));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, GET_ENERGY, "5600")));

        clientPair.appClient.createWidget(1, 21321, 1, "{\"id\":101, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":3}");
        clientPair.appClient.verifyResult(ok(6));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7, GET_ENERGY, "5400")));

        clientPair.appClient.deleteWidget(1, 101);
        clientPair.appClient.verifyResult(ok(8));

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(9, GET_ENERGY, "5600")));

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

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.createWidget(1, 21321, 1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.createWidget(1, 21321, 1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":2}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(4)));

        clientPair.appClient.updateWidget(1, "{\"id\":100, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 3\", \"type\":\"BUTTON\", \"pinType\":\"DIGITAL\", \"pin\":3}");
        clientPair.appClient.verifyResult(ok(5));

        clientPair.appClient.send("getWidget 1\0" + widgetId);
        deviceTiles = (DeviceTiles) JsonParser.parseWidget(clientPair.appClient.getBody(6), 0);
        assertNotNull(deviceTiles);
        assertEquals(widgetId, deviceTiles.id);
        assertNotNull(deviceTiles.templates);
        assertEquals(1, deviceTiles.templates.length);
        assertEquals("name", deviceTiles.templates[0].name);
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

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
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

        clientPair.appClient.createWidget(1, 21321, 1, menu);
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
        assertEquals("name", deviceTiles.templates[0].name);
        assertNotNull(deviceTiles.templates[0].widgets[0]);
        assertEquals(500, ((Menu) deviceTiles.templates[0].widgets[0]).labels.length);
    }

    @Test
    public void createpageTempalteWithIdAndSendEmail() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        long templateId = 1;
        clientPair.appClient.send("createTemplate " + b("1 " + widgetId + " ")
                + "{\"id\":" + templateId + ",\"templateId\":\"TMPL123\",\"name\":\"My New Template\",\"iconName\":\"iconName\",\"boardType\":\"ESP8266\",\"showDeviceName\":false,\"color\":0,\"tileColor\":0,\"fontSize\":\"LARGE\",\"showTileLabel\":false,\"pin\":{\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"VIRTUAL\",\"min\":0.0,\"max\":255.0}}");
        clientPair.appClient.verifyResult(ok(2));


        clientPair.appClient.send("email template 1 " + widgetId + " " + templateId);

        String expectedBody = "Template ID for {template_name} is: {template_id}.<br>\n" +
                "<br>\n" +
                "This ID should be added in <a href=\"https://github.com/blynkkk/blynk-library/blob/master/examples/Export_Demo/Template_ESP32/Settings.h\">Settings.h</a>. Simply change this line\n" +
                "<br>\n" +
                "<p>\n" +
                "    <i>\n" +
                "#define BOARD_TEMPLATE_ID             \"{template_id}\" // ID of the Tile Template. Can be found in Tile Template Settings\n" +
                "    </i>\n" +
                "</p>\n" +
                "Template ID is used during device provisioning process and defines which template will be assigned to the device of this particular type.\n" +
                "<br>\n" +
                "<br>\n" +
                "--<br>\n" +
                "<br>\n" +
                "Blynk Team<br>\n" +
                "<br>\n" +
                "<a href=\"https://www.blynk.io\">blynk.io</a>\n" +
                "<br>\n" +
                "<a href=\"https://www.blynk.cc\">blynk.cc</a>\n";

        expectedBody = expectedBody
                        .replace("{template_name}", "My New Template")
                        .replace("{template_id}", "TMPL123");

        verify(holder.mailWrapper, timeout(1000)).sendHtml(eq(getUserName()), eq("Template ID for My New Template"), eq(expectedBody));

    }

    @Test
    public void testAddAndRemoveTabs() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        Tabs tabs = new Tabs();
        tabs.id = 172649;
        tabs.width = 10;
        tabs.height = 1;
        tabs.tabs = new Tab[] {
                new Tab(0, "0"),
                new Tab(1, "1")
        };

        clientPair.appClient.createWidget(1, 21321, 1, tabs);
        clientPair.appClient.verifyResult(ok(3));

        Menu menu = new Menu();
        menu.id = 172650;
        menu.x = 2;
        menu.y = 34;
        menu.width = 6;
        menu.height = 1;
        menu.label = "Set Volume";
        menu.deviceId = 252521;
        menu.tabId = 0;

        clientPair.appClient.createWidget(1, 21321, 1, menu);
        clientPair.appClient.verifyResult(ok(4));

        menu = new Menu();
        menu.id = 172651;
        menu.x = 2;
        menu.y = 34;
        menu.width = 6;
        menu.height = 1;
        menu.label = "Set Volume";
        menu.deviceId = 252521;
        menu.tabId = 1;

        clientPair.appClient.createWidget(1, 21321, 1, menu);
        clientPair.appClient.verifyResult(ok(5));

        Tabs tabs2 = new Tabs();
        tabs2.id = 172648;
        tabs2.width = 10;
        tabs2.height = 1;
        tabs2.tabs = new Tab[] {
                new Tab(0, "0"),
                new Tab(1, "1")
        };

        clientPair.appClient.createWidget(1, tabs2);
        clientPair.appClient.verifyResult(ok(6));

        menu = new Menu();
        menu.id = 172652;
        menu.x = 2;
        menu.y = 34;
        menu.width = 6;
        menu.height = 1;
        menu.label = "Set Volume";
        menu.deviceId = 252521;
        menu.tabId = 0;

        clientPair.appClient.createWidget(1, menu);
        clientPair.appClient.verifyResult(ok(7));

        Menu menu2 = new Menu();
        menu2.id = 172653;
        menu2.x = 2;
        menu2.y = 34;
        menu2.width = 6;
        menu2.height = 1;
        menu2.label = "Set Volume";
        menu2.deviceId = 252521;
        menu2.tabId = 1;

        clientPair.appClient.createWidget(1, menu2);
        clientPair.appClient.verifyResult(ok(8));

        clientPair.appClient.deleteWidget(1, tabs.id);
        clientPair.appClient.verifyResult(ok(9));

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = clientPair.appClient.parseDash(10);
        assertNotNull(dashBoard);
        Tabs dashTabs = dashBoard.getWidgetByType(Tabs.class);
        assertNotNull(dashTabs);
        assertEquals(2, dashTabs.tabs.length);
        assertNotNull(dashBoard.getWidgetById(menu.id));
        assertNotNull(dashBoard.getWidgetById(menu2.id));
        DeviceTiles deviceTiles1 = dashBoard.getWidgetByType(DeviceTiles.class);
        assertNotNull(deviceTiles1);
        assertNull(deviceTiles1.getWidgetById(tabs.id));
        assertEquals(1, deviceTiles1.templates[0].widgets.length);
        assertEquals(0, deviceTiles1.templates[0].getWidgetIndexByIdOrThrow(172650));
        assertTrue(deviceTiles1.templates[0].widgets[0] instanceof Menu);
    }

    @Test
    public void testAddAndRemoveTabs2() throws Exception {
        Tabs tabs = new Tabs();
        tabs.id = 172648;
        tabs.width = 10;
        tabs.height = 1;
        tabs.tabs = new Tab[] {
                new Tab(0, "0"),
                new Tab(1, "1")
        };

        clientPair.appClient.createWidget(1, tabs);
        clientPair.appClient.verifyResult(ok(1));

        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(2));

        Button button = new Button();
        button.id = 172649;
        button.x = 2;
        button.y = 34;
        button.width = 6;
        button.height = 1;
        button.label = "Set Volume";
        button.deviceId = 0;
        button.tabId = 0;

        clientPair.appClient.createWidget(1, button);
        clientPair.appClient.verifyResult(ok(3));

        Button button2 = new Button();
        button2.id = 172650;
        button2.x = 2;
        button2.y = 34;
        button2.width = 6;
        button2.height = 1;
        button2.label = "Set Volume";
        button2.deviceId = 0;
        button2.tabId = 1;

        clientPair.appClient.createWidget(1, button2);
        clientPair.appClient.verifyResult(ok(4));

        ButtonTileTemplate tileTemplate = new ButtonTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, false, false, null, null);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(5));

        tileTemplate = new ButtonTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 1, PinType.VIRTUAL),
                false, false, false, null, null);

        clientPair.appClient.updateTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(6));

        Tabs tabs2 = new Tabs();
        tabs2.id = 172651;
        tabs2.width = 10;
        tabs2.height = 1;
        tabs2.tabs = new Tab[] {
                new Tab(0, "0"),
                new Tab(1, "1")
        };

        clientPair.appClient.createWidget(1, 21321, 1, tabs2);
        clientPair.appClient.verifyResult(ok(7));

        Button button3 = new Button();
        button3.id = 172652;
        button3.x = 2;
        button3.y = 34;
        button3.width = 6;
        button3.height = 1;
        button3.label = "Set Volume";
        button3.deviceId = 0;
        button3.tabId = 0;

        clientPair.appClient.createWidget(1, 21321, 1, button3);
        clientPair.appClient.verifyResult(ok(8));

        Button button4 = new Button();
        button4.id = 172653;
        button4.x = 2;
        button4.y = 34;
        button4.width = 6;
        button4.height = 1;
        button4.label = "Set Volume";
        button4.deviceId = 0;
        button4.tabId = 1;

        clientPair.appClient.createWidget(1, 21321, 1, button4);
        clientPair.appClient.verifyResult(ok(9));

        clientPair.appClient.deleteWidget(1, tabs2.id);
        clientPair.appClient.verifyResult(ok(10));

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = clientPair.appClient.parseDash(11);
        assertNotNull(dashBoard);
        Tabs searchTabs = (Tabs) dashBoard.getWidgetById(tabs.id);
        assertNotNull(searchTabs);
        assertNotNull(dashBoard.getWidgetById(button.id));
        assertNotNull(dashBoard.getWidgetById(button2.id));
    }

    @Test
    public void testAddAndUpdateTabs() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, null, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        Tabs tabs = new Tabs();
        tabs.id = 172649;
        tabs.width = 10;
        tabs.height = 1;
        tabs.tabs = new Tab[] {
                new Tab(0, "0"),
                new Tab(1, "1")
        };

        clientPair.appClient.createWidget(1, 21321, 1, tabs);
        clientPair.appClient.verifyResult(ok(3));

        Menu menu = new Menu();
        menu.id = 172650;
        menu.x = 2;
        menu.y = 34;
        menu.width = 6;
        menu.height = 1;
        menu.label = "Set Volume";
        menu.deviceId = 252521;
        menu.tabId = 0;

        clientPair.appClient.createWidget(1, 21321, 1, menu);
        clientPair.appClient.verifyResult(ok(4));

        menu = new Menu();
        menu.id = 172651;
        menu.x = 2;
        menu.y = 34;
        menu.width = 6;
        menu.height = 1;
        menu.label = "Set Volume";
        menu.deviceId = 252521;
        menu.tabId = 1;

        clientPair.appClient.createWidget(1, 21321, 1, menu);
        clientPair.appClient.verifyResult(ok(5));

        Tabs tabs2 = new Tabs();
        tabs2.id = 172648;
        tabs2.width = 10;
        tabs2.height = 1;
        tabs2.tabs = new Tab[] {
                new Tab(0, "0"),
                new Tab(1, "1")
        };

        clientPair.appClient.createWidget(1, tabs2);
        clientPair.appClient.verifyResult(ok(6));

        menu = new Menu();
        menu.id = 172652;
        menu.x = 2;
        menu.y = 34;
        menu.width = 6;
        menu.height = 1;
        menu.label = "Set Volume";
        menu.deviceId = 252521;
        menu.tabId = 0;

        clientPair.appClient.createWidget(1, menu);
        clientPair.appClient.verifyResult(ok(7));

        Menu menu2 = new Menu();
        menu2.id = 172653;
        menu2.x = 2;
        menu2.y = 34;
        menu2.width = 6;
        menu2.height = 1;
        menu2.label = "Set Volume";
        menu2.deviceId = 252521;
        menu2.tabId = 1;

        clientPair.appClient.createWidget(1, menu2);
        clientPair.appClient.verifyResult(ok(8));

        tabs.tabs = new Tab[] {
                new Tab(0, "0")
        };

        clientPair.appClient.updateWidget(1, tabs);
        clientPair.appClient.verifyResult(ok(9));

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = clientPair.appClient.parseDash(10);
        assertNotNull(dashBoard);
        Tabs dashTabs = dashBoard.getWidgetByType(Tabs.class);
        assertNotNull(dashTabs);
        assertEquals(2, dashTabs.tabs.length);
        assertNotNull(dashBoard.getWidgetById(menu.id));
        assertNotNull(dashBoard.getWidgetById(menu2.id));
        DeviceTiles deviceTiles1 = dashBoard.getWidgetByType(DeviceTiles.class);
        assertNotNull(deviceTiles1.getWidgetById(tabs.id));
        assertTrue(deviceTiles1.getWidgetById(tabs.id) instanceof Tabs);
        assertEquals(2, deviceTiles1.templates[0].widgets.length);
        int menuWidgetIndex = deviceTiles1.templates[0].getWidgetIndexByIdOrThrow(172650);
        assertEquals(1, menuWidgetIndex);
        assertTrue(deviceTiles1.templates[0].widgets[menuWidgetIndex] instanceof Menu);
    }

    @Test
    public void testGetPinViaHttpApiWorksForDeviceTiles() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new ButtonTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 111, PinType.VIRTUAL),
                false, false, false, null, null);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices(3);
        Device device = devices[0];
        assertEquals(0, device.id);

        clientPair.appClient.send("hardware 1-0 vw 111 1");

        AsyncHttpClient httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );

        String httpsServerUrl = String.format("http://localhost:%s/", properties.getHttpPort());
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + device.token + "/get/v111").execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        assertEquals("1", response.getResponseBody());
        httpclient.close();
    }

    @Test
    public void testDeviceTileIsUpdatedFromHardware() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new ButtonTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 111, PinType.VIRTUAL),
                false, false, false, null, null);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.hardwareClient.send("hardware vw 111 1");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 111 1"));

        clientPair.appClient.send("loadProfileGzipped 1");
        DashBoard dashBoard = clientPair.appClient.parseDash(4);
        assertNotNull(dashBoard);
        deviceTiles = dashBoard.getWidgetByType(DeviceTiles.class);
        assertNotNull(deviceTiles);
        assertEquals(1, deviceTiles.tiles.length);
        assertEquals("1", deviceTiles.tiles[0].dataStream.value);
    }

    @Test
    public void testDeviceTileAndWidgetWithinTemplateHasSamePin() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        DataStream dataStream = new DataStream((short) 5, PinType.VIRTUAL);
        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.hardwareClient.send("hardware vw 5 111");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 5 111"));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = dataStream.pin;
        valueDisplay.pinType = dataStream.pinType;

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 5 111")));

        clientPair.hardwareClient.send("hardware vw 5 112");
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 5 112"));


        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 5 112")));

        clientPair.hardwareClient.sync(PinType.VIRTUAL, 5);
        clientPair.hardwareClient.verifyResult(produce(3, HARDWARE, b("vw 5 112")));
    }

    @Test
    public void testDeviceTileAndWidgetWithinTemplateHasSamePinAndUpdateFromApp() throws Exception {
        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        DataStream dataStream = new DataStream((short) 5, PinType.VIRTUAL);
        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        NumberInput numberInput = new NumberInput();
        numberInput.width = 2;
        numberInput.height = 2;
        numberInput.pin = dataStream.pin;
        numberInput.pinType = dataStream.pinType;

        clientPair.appClient.createWidget(1, deviceTiles.id, tileTemplate.id, numberInput);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("hardware 1-0 vw 5 111");
        clientPair.hardwareClient.verifyResult(hardware(4, "vw 5 111"));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 5 111")));
    }

    @Test
    public void testDeviceTileAndWidgetWithinTemplateHasSamePin2() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, null,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        DataStream dataStream = new DataStream((short) 5, PinType.VIRTUAL);

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = dataStream.pin;
        valueDisplay.pinType = dataStream.pinType;

        clientPair.appClient.createWidget(1, deviceTiles.id, 1, valueDisplay);
        clientPair.appClient.verifyResult(ok(3));

        //send value after we have tile for that pin
        clientPair.hardwareClient.send("hardware vw 5 111");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 5 111"));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 5 111")));

        clientPair.hardwareClient.send("hardware vw 5 112");
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 5 112"));


        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

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
        clientPair.appClient.verifyResult(appSync(b("1-0 vw 5 112")));
    }

    @Test
    public void testDeviceTileAndWidgetWithMultipleValues() throws Exception {
        var deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        var tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, new DataStream((short) 5, PinType.VIRTUAL),
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, deviceTiles.id, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        var terminal = new Terminal();
        terminal.width = 2;
        terminal.height = 2;
        terminal.pin = 6;
        terminal.pinType = PinType.VIRTUAL;

        clientPair.appClient.createWidget(1, deviceTiles.id, 1, terminal);
        clientPair.appClient.verifyResult(ok(3));

        //send value after we have tile for that pin
        clientPair.hardwareClient.send("hardware vw 6 111");
        clientPair.hardwareClient.send("hardware vw 6 112");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 6 111"));
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 6 112"));

        clientPair.appClient.reset();
        clientPair.appClient.sync(1, 0);

        verify(clientPair.appClient.responseMock, timeout(500).times(11 + 2)).channelRead(any(), any());

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

        clientPair.appClient.verifyResult(appSync(b("1-0 vm 6 111 112")));
    }

    @Test
    public void updateViaHttpAPIWorksForDeviceTiles() throws Exception {
        long widgetId = 21321;

        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = widgetId;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        DataStream dataStream = new DataStream((short) 5, PinType.VIRTUAL);
        TileTemplate tileTemplate = new PageTileTemplate(1,
                null, new int[] {0}, "name", "name", "iconName", BoardType.ESP8266, dataStream,
                false, null, null, null, 0, 0, FontSize.LARGE, false, 2);

        clientPair.appClient.createTemplate(1, widgetId, tileTemplate);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices(3);
        Device device = devices[0];
        assertNotNull(device);
        assertNotNull(device.token);

        AsyncHttpClient httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );

        String httpsServerUrl = String.format("http://localhost:%s/", properties.getHttpPort());
        Future<Response> f = httpclient
                .prepareGet(httpsServerUrl + device.token + "/update/v5?value=111")
                .execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());

        clientPair.appClient.verifyResult(hardware(111, "1-0 vw 5 111"));
        httpclient.close();
    }
}
