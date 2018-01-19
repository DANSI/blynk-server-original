package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.api.http.AppAndHttpsServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.hardware.HardwareServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Paths;

import static cc.blynk.server.core.model.serialization.JsonParser.MAPPER;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
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
        this.hardwareServer = new HardwareServer(holder).start();
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
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        clientPair.appClient.send("createWidget 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, ILLEGAL_COMMAND)));

        clientPair.appClient.send("createWidget 1" + "\0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, ILLEGAL_COMMAND)));

        clientPair.appClient.send("createWidget 1" + "\0" + "{}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, ILLEGAL_COMMAND_BODY)));

        //very large widget
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10 * 1024 + 1; i++) {
            sb.append("a");
        }

        clientPair.appClient.send("createWidget 1" + "\0" + sb.toString());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, NOT_ALLOWED)));
    }

    @Test
    public void testCanCreateWebHookWithScheme() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":1111, \"width\":1, \"height\":1,\"url\":\"http://123.com\",\"type\":\"WEBHOOK\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("createWidget 1\0{\"id\":1113, \"width\":1, \"height\":1,\"url\":\"https://123.com\",\"type\":\"WEBHOOK\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testWidgetAlreadyExists() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":1, \"width\":1, \"height\":1,\"type\":\"BUTTON\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));
    }

    @Test
    public void testWidgetWrongSize() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":22222, \"width\":1, \"height\":0,\"type\":\"BUTTON\"}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));
    }

    @Test
    public void testCreateWidgetAndRemove() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"frequency\":1000,\"isAxisFlipOn\":false,\"color\":600084223,\"isDefaultColor\":true,\"rangeMappingOn\":false,\"pin\":-1,\"pwmMode\":false,\"deviceId\":0,\"height\":3,\"id\":82561,\"tabId\":1,\"type\":\"VERTICAL_LEVEL_DISPLAY\",\"width\":1,\"x\":0,\"y\":6,\"min\":0,\"max\":1023}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("deleteWidget 1 82561");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testCreateWidgetAndRemoveWithDeviceTiles() throws Exception {
        DeviceTiles deviceTiles = new DeviceTiles();
        deviceTiles.id = 21321;
        deviceTiles.x = 8;
        deviceTiles.y = 8;
        deviceTiles.width = 50;
        deviceTiles.height = 100;

        clientPair.appClient.send("createWidget 1\0" + MAPPER.writeValueAsString(deviceTiles));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("createWidget 1\0{\"frequency\":1000,\"isAxisFlipOn\":false,\"color\":600084223,\"isDefaultColor\":true,\"rangeMappingOn\":false,\"pin\":-1,\"pwmMode\":false,\"deviceId\":0,\"height\":3,\"id\":82561,\"tabId\":1,\"type\":\"VERTICAL_LEVEL_DISPLAY\",\"width\":1,\"x\":0,\"y\":6,\"min\":0,\"max\":1023}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("deleteWidget 1 82561");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));
    }

}
