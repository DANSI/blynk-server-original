package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.ValueDisplay;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Paths;

import static cc.blynk.integration.TestUtil.appSync;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.illegalCommand;
import static cc.blynk.integration.TestUtil.illegalCommandBody;
import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static org.junit.Assert.assertEquals;
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
public class WidgetWorkflowTest extends SingleServerInstancePerTest {

    @Before
    public void deleteFolder() throws Exception {
        Files.deleteIfExists(Paths.get(getDataFolder(), "blynk", "userProfiles"));
    }

    @Test
    public void testCorrectBehaviourOnWrongInput() throws Exception {
        clientPair.appClient.send("createWidget ");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(illegalCommand(1)));

        clientPair.appClient.send("createWidget 1");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(illegalCommand(2)));

        clientPair.appClient.send("createWidget 1" + "\0");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(illegalCommand(3)));

        clientPair.appClient.send("createWidget 1" + "\0" + "{}");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(notAllowed(4)));

        //very large widget
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10 * 1024 + 1; i++) {
            sb.append("a");
        }

        clientPair.appClient.createWidget(1, sb.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, NOT_ALLOWED)));
    }

    @Test
    public void testCanCreateWebHookWithScheme() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":1111, \"width\":1, \"height\":1,\"url\":\"http://123.com\",\"type\":\"WEBHOOK\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.createWidget(1, "{\"id\":1113, \"width\":1, \"height\":1,\"url\":\"https://123.com\",\"type\":\"WEBHOOK\"}");
        clientPair.appClient.verifyResult(ok(2));
    }

    @Test
    public void testWidgetAlreadyExists() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":1, \"width\":1, \"height\":1,\"type\":\"BUTTON\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(1)));
    }

    @Test
    public void testWidgetWrongSize() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":22222, \"width\":1, \"height\":0,\"type\":\"BUTTON\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(1)));
    }

    @Test
    public void testCreateWidgetBadFormat() throws Exception {
        clientPair.appClient.createWidget(1, ":600084223,\"isDefaultColor\":true,\"rangeMappingOn\":false,\"pin\":-1,\"pwmMode\":false,\"deviceId\":0,\"height\":3,\"id\":82561,\"tabId\":1,\"type\":\"VERTICAL_LEVEL_DISPLAY\",\"width\":1,\"x\":0,\"y\":6,\"min\":0,\"max\":1023}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommandBody(1)));
    }

    @Test
    public void testCreateWidgetAndRemove() throws Exception {
        clientPair.appClient.createWidget(1, "{\"frequency\":1000,\"isAxisFlipOn\":false,\"color\":600084223,\"isDefaultColor\":true,\"rangeMappingOn\":false,\"pin\":-1,\"pwmMode\":false,\"deviceId\":0,\"height\":3,\"id\":82561,\"tabId\":1,\"type\":\"VERTICAL_LEVEL_DISPLAY\",\"width\":1,\"x\":0,\"y\":6,\"min\":0,\"max\":1023}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.deleteWidget(1, 82561);
        clientPair.appClient.verifyResult(ok(2));
    }

    @Test
    public void testPinStorageIsCleanedOnWidgetRemoval() throws Exception {
        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 82561;
        valueDisplay.width = 2;
        valueDisplay.height = 2;
        valueDisplay.pin = 111;
        valueDisplay.pinType = PinType.VIRTUAL;
        valueDisplay.deviceId = 200_000;

        clientPair.appClient.createWidget(1, valueDisplay);
        clientPair.appClient.verifyResult(ok(1));

        DeviceSelector deviceSelector = new DeviceSelector();
        deviceSelector.id = 200000;
        deviceSelector.x = 0;
        deviceSelector.y = 0;
        deviceSelector.width = 1;
        deviceSelector.height = 1;
        deviceSelector.deviceIds = new int[] {0};

        clientPair.appClient.createWidget(1, deviceSelector);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.hardwareClient.send("hardware vw 111 test");
        clientPair.appClient.verifyResult(hardware(1, "1-0 vw 111 test"));

        clientPair.appClient.sync(1, 0);
        clientPair.appClient.verifyResult(appSync("1-0 vw 111 test"));
        clientPair.appClient.reset();

        clientPair.appClient.deleteWidget(1, 82561);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.sync(1, 0);
        clientPair.appClient.neverAfter(500, appSync("1-0 vw 111 test"));
    }

    @Test
    public void testCreateWidgetAndRemoveWithDeviceTiles() throws Exception {
        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.createWidget(1, deviceTiles);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.createWidget(1, "{\"frequency\":1000,\"isAxisFlipOn\":false,\"color\":600084223,\"isDefaultColor\":true,\"rangeMappingOn\":false,\"pin\":-1,\"pwmMode\":false,\"deviceId\":0,\"height\":3,\"id\":82561,\"tabId\":1,\"type\":\"VERTICAL_LEVEL_DISPLAY\",\"width\":1,\"x\":0,\"y\":6,\"min\":0,\"max\":1023}");
        clientPair.appClient.verifyResult(ok(2));

        clientPair.appClient.deleteWidget(1, 82561);
        clientPair.appClient.verifyResult(ok(3));
    }

    @Test
    // https://github.com/blynkkk/blynk-server/issues/1266
    public void testWidgetValueNotChangedAfterUpdate() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":82561, \"width\":1, \"height\":1,\"type\":\"BUTTON\", \"value\":\"1\"}");
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(2);
        assertNotNull(profile);
        Widget widget = profile.dashBoards[0].getWidgetById(82561);
        assertEquals("1", ((OnePinWidget) widget).value);

        clientPair.appClient.updateWidget(1, "{\"id\":82561, \"width\":2, \"height\":2,\"type\":\"BUTTON\"}");

        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(4);
        widget = profile.dashBoards[0].getWidgetById(82561);
        assertEquals("1", ((OnePinWidget) widget).value);
    }

}
