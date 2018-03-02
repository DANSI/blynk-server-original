package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Paths;

import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
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
public class WidgetWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new AppAndHttpsServer(holder).start();

        this.clientPair = initAppAndHardPair();
        Files.deleteIfExists(Paths.get(getDataFolder(), "blynk", "userProfiles"));
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }


    @Test
    public void testCorrectBehaviourOnWrongInput() throws Exception {
        clientPair.appClient.send("createWidget ");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(1)));

        clientPair.appClient.send("createWidget 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(2)));

        clientPair.appClient.send("createWidget 1" + "\0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(illegalCommand(3)));

        clientPair.appClient.send("createWidget 1" + "\0" + "{}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, ILLEGAL_COMMAND_BODY)));

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

}
