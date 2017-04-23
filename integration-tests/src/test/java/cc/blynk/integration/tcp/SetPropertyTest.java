package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.others.Player;
import cc.blynk.server.core.model.widgets.ui.Menu;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.SetWidgetPropertyMessage;
import cc.blynk.server.hardware.HardwareServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SetPropertyTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;


    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();
        this.clientPair = initAppAndHardPair("user_profile_json.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testSetWidgetProperty() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte)4, PinType.VIRTUAL);
        assertNotNull(widget);
        assertEquals("Some Text", widget.label);

        clientPair.hardwareClient.send("setProperty 4 label MyNewLabel");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(1, b("1 4 label MyNewLabel"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody());

        widget = profile.dashBoards[0].findWidgetByPin(0, (byte)4, PinType.VIRTUAL);
        assertNotNull(widget);
        assertEquals("MyNewLabel", widget.label);
    }

    @Test
    public void testSetButtonProperty() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"onLabel\":\"On\", \"offLabel\":\"Off\" , \"type\":\"BUTTON\", \"pinType\":\"VIRTUAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("setProperty 17 onLabel вкл");
        clientPair.hardwareClient.send("setProperty 17 offLabel выкл");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(1, b("1 17 onLabel вкл"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(2, b("1 17 offLabel выкл"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 17, PinType.VIRTUAL);
        assertNotNull(widget);
        assertTrue(widget instanceof Button);
        Button button = (Button) widget;

        assertEquals("вкл", button.onLabel);
        assertEquals("выкл", button.offLabel);
    }


    @Test
    public void testSetBooleanProperty() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"PLAYER\", \"pinType\":\"VIRTUAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("setProperty 17 isOnPlay true");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(1, b("1 17 isOnPlay true"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 17, PinType.VIRTUAL);
        assertNotNull(widget);
        assertTrue(widget instanceof Player);
        Player playerWidget = (Player) widget;

        assertTrue(playerWidget.isOnPlay);
    }

    @Test
    public void testSetStringArrayWidgetPropertyForMenu() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"width\":1, \"height\":1, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"MENU\", \"pinType\":\"VIRTUAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("setProperty 17 labels label1 label2 label3");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(1, b("1 17 labels label1 label2 label3"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 17, PinType.VIRTUAL);
        assertNotNull(widget);
        assertTrue(widget instanceof Menu);
        Menu menuWidget = (Menu) widget;

        assertArrayEquals(new String[] {"label1", "label2", "label3"}, menuWidget.labels);


    }

    @Test
    public void testSetWrongWidgetProperty() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 4, PinType.VIRTUAL);
        assertEquals(widget.label, "Some Text");

        clientPair.hardwareClient.send("setProperty 4 YYY MyNewLabel");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND_BODY)));

        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody(2));
        widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 4, PinType.VIRTUAL);

        assertEquals(widget.label, "Some Text");
    }

    @Test
    public void testSetWrongWidgetProperty2() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 4, PinType.VIRTUAL);
        assertEquals(widget.x, 1);

        clientPair.hardwareClient.send("setProperty 4 x 0");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND_BODY)));

        clientPair.appClient.send("loadProfileGzipped");
        profile = parseProfile(clientPair.appClient.getBody(2));
        widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 4, PinType.VIRTUAL);

        assertEquals(widget.x, 1);
    }

    @Test
    public void testSetColorForWidget() throws Exception {
        clientPair.hardwareClient.send("setProperty 4 color #23C48E");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(1, b("1 4 color #23C48E"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());
        profile.dashBoards[0].updatedAt = 0;

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 4, PinType.VIRTUAL);
        assertEquals(600084223, widget.color);
    }

    @Test
    public void setMinMaxProperty() throws Exception {
        clientPair.hardwareClient.send("setProperty 4 min 10");
        clientPair.hardwareClient.send("setProperty 4 max 20");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(1, b("1 4 min 10"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SetWidgetPropertyMessage(2, b("1 4 max 20"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = parseProfile(clientPair.appClient.getBody());
        profile.dashBoards[0].updatedAt = 0;

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (byte) 4, PinType.VIRTUAL);
        assertEquals(10, ((OnePinWidget) widget).min);
        assertEquals(20, ((OnePinWidget) widget).max);
    }

    @Test
    public void testSetColorShouldNotWorkForNonActiveProject() throws Exception {
        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("setProperty 4 color #23C48E");
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(ok(1)));
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), eq(new SetWidgetPropertyMessage(1, b("1 4 color #23C48E"))));
    }

}
