package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoadProfileGzippedBinaryMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ShareProfileWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    private static OnePinWidget getWidgetByPin(Profile profile, int pin) {
        for (Widget widget : profile.dashBoards[0].widgets) {
            if (widget instanceof OnePinWidget) {
                OnePinWidget onePinWidget = (OnePinWidget) widget;
                if (onePinWidget.pin != -1 && onePinWidget.pin == pin) {
                    return onePinWidget;
                }
            }
        }
        return null;
    }

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
        this.appServer = new AppServer(holder).start(transportTypeHolder);
        this.clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        this.appServer.stop();
        this.hardwareServer.stop();
        this.clientPair.stop();
    }

    @Test
    public void testGetShareTokenNoDashId() throws Exception {
        clientPair.appClient.send("getShareToken");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));
    }

    @Test
    public void testGetShareToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        ClientPair clientPair2 = initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, "dima2@mail.ua 1", "user_profile_json_2.txt", properties);
        clientPair2.appClient.send("getSharedDash " + token);

        String dashboard = clientPair2.appClient.getBody();

        assertNotNull(dashboard);
        Profile profile = JsonParser.parseProfile(readTestUserProfile(), 1);
        Twitter twitter = profile.dashBoards[0].getWidgetByType(Twitter.class);
        twitter.cleanPrivateData();
        Notification notification = profile.dashBoards[0].getWidgetByType(Notification.class);
        notification.cleanPrivateData();

        //one field update, cause it is hard to compare.
        DashBoard temp = JsonParser.parseDashboard(dashboard, 1);
        profile.dashBoards[0].updatedAt = temp.updatedAt;


        assertEquals(profile.dashBoards[0].toString(), dashboard);
        //System.out.println(dashboard);
    }

    @Test
    public void getShareTokenAndLoginViaIt() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());


        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, SYNC, b("1 vw 1 1"))));

        appClient2.send("hardware 1 vw 2 2");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, SYNC, b("1 vw 2 2"))));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();
        appClient2.reset();

        appClient2.send("hardware 1 ar 7");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("ar 7"))));

        clientPair.appClient.send("hardware 1 ar 7");
        verify(appClient2.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(2, HARDWARE, b("ar 7"))));

        clientPair.hardwareClient.reset();
        clientPair.hardwareClient.send("ping");

        appClient2.send("hardware 1 pm 2 2");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock,  after(500).atMost(1)).channelRead(any(), any());

        clientPair.appClient.send("hardware 1 ar 7");
        verify(appClient2.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("ar 7"))));

        clientPair.appClient.send("hardware 1 pm 2 2");
        verify(appClient2.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("pm 2 2"))));
    }

    @Test
    public void checkStateWasChanged() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, SYNC, b("1 vw 1 1"))));

        appClient2.send("hardware 1 vw 2 2");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, SYNC, b("1 vw 2 2"))));

        clientPair.appClient.reset();
        appClient2.reset();

        //check from master side
        clientPair.appClient.send("hardware 1 aw 3 1");
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 1"))));

        clientPair.appClient.send("loadProfileGzipped");
        String profileString = clientPair.appClient.getBody();
        assertNotNull(profileString);
        Profile profile = JsonParser.parseProfile(profileString, 0);

        OnePinWidget tmp = getWidgetByPin(profile, 3);

        assertNotNull(tmp);
        assertEquals("1", tmp.value);


        //check from slave side
        appClient2.send("hardware 1 aw 3 150");
        verify(clientPair.hardwareClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 150"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profileString = clientPair.appClient.getBody();
        assertNotNull(profileString);
        profile = JsonParser.parseProfile(profileString, 0);

        tmp = getWidgetByPin(profile, 3);

        assertNotNull(tmp);
        assertEquals("150", tmp.value);

        //check from hard side
        clientPair.hardwareClient.send("hardware aw 3 151");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 3 151"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profileString = clientPair.appClient.getBody();
        assertNotNull(profileString);
        profile = JsonParser.parseProfile(profileString, 0);

        tmp = getWidgetByPin(profile, 3);

        assertNotNull(tmp);
        assertEquals("151", tmp.value);
    }

    @Test
    public void checkSharingMessageWasReceived() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("sharing 1 off");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, SHARING, b("1 off"))));
    }

    @Test
    public void checkBothClientsReceiveMessage() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        //check from hard side
        clientPair.hardwareClient.send("hardware aw 3 151");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 3 151"))));
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("1 aw 3 151"))));

        clientPair.hardwareClient.send("hardware aw 3 152");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, HARDWARE, b("1 aw 3 152"))));
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, HARDWARE, b("1 aw 3 152"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        String profileString = clientPair.appClient.getBody();
        assertNotNull(profileString);
        Profile profile = JsonParser.parseProfile(profileString, 0);

        OnePinWidget tmp = getWidgetByPin(profile, 3);

        assertNotNull(tmp);
        assertEquals("152", tmp.value);
    }

    @Test
    public void wrongSharedToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token+"a" + " Android 24");

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));
    }

    @Test
    public void revokeSharedToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();
        appClient2.reset();

        assertFalse(clientPair.appClient.isClosed());
        assertFalse(appClient2.isClosed());

        clientPair.appClient.send("refreshShareToken 1");
        token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, NOT_ALLOWED)));

        assertFalse(clientPair.appClient.isClosed());
        assertTrue(appClient2.isClosed());

        TestAppClient appClient3 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient3.start();
        appClient3.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient3.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
    }

    @Test
    public void testDeactivateAndActivateForSubscriptions() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        TestAppClient appClient3 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient3.start();
        appClient3.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "1")));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "1")));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(3, ACTIVATE_DASHBOARD, "1")));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(3, ACTIVATE_DASHBOARD, "1")));
    }

    @Test
    public void loadGzippedProfileForSharedBoard() throws Exception{
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        String body = clientPair.appClient.getBody();

        appClient2.send("loadProfileGzipped");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new LoadProfileGzippedBinaryMessage(2, body)));
    }


    @Test
    public void testGetShareTokenAndRefresh() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        ClientPair clientPair2 = initAppAndHardPair("localhost", tcpAppPort, tcpHardPort, "dima2@mail.ua 1", "user_profile_json_2.txt", properties);
        clientPair2.appClient.send("getSharedDash " + token);

        String dashboard = clientPair2.appClient.getBody();

        assertNotNull(dashboard);

        clientPair.appClient.reset();
        clientPair.appClient.send("refreshShareToken 1");
        String refreshedToken = clientPair.appClient.getBody();
        assertNotNull(refreshedToken);
        assertNotEquals(refreshedToken, token);

        clientPair.appClient.reset();
        clientPair.appClient.send("getSharedDash " + token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, INVALID_TOKEN)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getSharedDash " + refreshedToken);

        dashboard = clientPair.appClient.getBody();

        assertNotNull(dashboard);
        Profile profile = JsonParser.parseProfile(readTestUserProfile(), 1);
        Twitter twitter = profile.dashBoards[0].getWidgetByType(Twitter.class);
        twitter.cleanPrivateData();
        Notification notification = profile.dashBoards[0].getWidgetByType(Notification.class);
        notification.cleanPrivateData();

        //one field update, cause it is hard to compare.
        DashBoard temp = JsonParser.parseDashboard(dashboard, 1);
        profile.dashBoards[0].updatedAt = temp.updatedAt;

        assertEquals(profile.dashBoards[0].toString(), dashboard);
        //System.out.println(dashboard);
    }

    @Test
    public void testMasterMasterSyncWorksWithoutToken() throws Exception {
        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient2.start();
        appClient2.send("login " + "dima@mail.ua 1 Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, SYNC, b("1 vw 1 1"))));

        appClient2.send("hardware 1 vw 2 2");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, SYNC, b("1 vw 2 2"))));
    }


}
