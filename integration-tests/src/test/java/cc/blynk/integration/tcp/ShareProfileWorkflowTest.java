package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DashboardSettings;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinActionType;
import cc.blynk.server.core.model.widgets.others.eventor.model.condition.number.GreaterThan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.parseProfile;
import static cc.blynk.integration.TestUtil.readTestUserProfile;
import static cc.blynk.integration.TestUtil.setProperty;
import static cc.blynk.server.core.protocol.enums.Command.ACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.DEACTIVATE_DASHBOARD;
import static cc.blynk.server.core.protocol.enums.Command.GET_ENERGY;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.SHARING;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
public class ShareProfileWorkflowTest extends SingleServerInstancePerTest {

    private static OnePinWidget getWidgetByPin(Profile profile, int pin) {
        for (Widget widget : profile.dashBoards[0].widgets) {
            if (widget instanceof OnePinWidget) {
                OnePinWidget onePinWidget = (OnePinWidget) widget;
                if (onePinWidget.pin != DataStream.NO_PIN && onePinWidget.pin == pin) {
                    return onePinWidget;
                }
            }
        }
        return null;
    }

    @Test
    public void testGetShareTokenNoDashId() throws Exception {
        clientPair.appClient.send("getShareToken");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(notAllowed(1)));
    }

    @Test
    public void testGetShareToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient2.send("loadProfileGzipped");
        Profile serverProfile = appClient2.parseProfile(2);
        DashBoard serverDash = serverProfile.dashBoards[0];

        Profile profile = parseProfile(readTestUserProfile());
        Twitter twitter = profile.dashBoards[0].getTwitterWidget();
        clearPrivateData(twitter);
        Notification notification = profile.dashBoards[0].getNotificationWidget();
        clearPrivateData(notification);

        profile.dashBoards[0].updatedAt = serverDash.updatedAt;
        assertNull(serverDash.sharedToken);
        serverDash.devices = null;
        profile.dashBoards[0].devices = null;

        assertEquals(profile.dashBoards[0].toString(), serverDash.toString());

        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(2);

        profile.dashBoards[0].updatedAt = 0;
        Notification originalNotification = profile.dashBoards[0].getNotificationWidget();
        assertNotNull(originalNotification);
        assertEquals(1, originalNotification.androidTokens.size());
        assertEquals("token", originalNotification.androidTokens.get("uid"));

        Twitter originalTwitter = profile.dashBoards[0].getTwitterWidget();
        assertNotNull(originalTwitter);
        assertEquals("token", originalTwitter.token);
        assertEquals("secret", originalTwitter.secret);
    }

    @Test
    public void getShareTokenAndLoginViaIt() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());


        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, APP_SYNC, b("1 vw 1 1"))));

        appClient2.send("hardware 1 vw 2 2");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, APP_SYNC, b("1 vw 2 2"))));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();
        appClient2.reset();

        appClient2.send("hardware 1 ar 30");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(1, HARDWARE, b("ar 30"))));

        clientPair.appClient.send("hardware 1 ar 30");
        verify(appClient2.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(2, HARDWARE, b("ar 30"))));

        clientPair.hardwareClient.reset();
        clientPair.hardwareClient.send("ping");

        appClient2.send("hardware 1 pm 2 2");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock,  after(500).atMost(1)).channelRead(any(), any());

        clientPair.appClient.send("hardware 1 ar 30");
        verify(appClient2.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(2, HARDWARE, b("ar 30"))));

        clientPair.appClient.send("hardware 1 pm 2 2");
        verify(appClient2.responseMock, after(250).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(250).never()).channelRead(any(), eq(produce(3, HARDWARE, b("pm 2 2"))));
    }

    @Test
    public void getShareMultipleTokensAndLoginViaIt() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token1 = clientPair.appClient.getBody();
        assertNotNull(token1);
        assertEquals(32, token1.length());

        DashBoard dash = new DashBoard();
        dash.id = 2;
        dash.name = "test";
        clientPair.appClient.createDash(dash);
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        DashboardSettings settings = new DashboardSettings(dash.name,
                true, Theme.Blynk, false, false, false, false, 0, false);

        clientPair.appClient.send("updateSettings 2\0" + JsonParser.toJson(settings));
        clientPair.appClient.verifyResult(ok(3));

        clientPair.appClient.send("getShareToken 2");

        String token2 = clientPair.appClient.getBody(4);
        assertNotNull(token2);
        assertEquals(32, token2.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token1 + " Android 24");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TestAppClient appClient3 = new TestAppClient(properties);
        appClient3.start();
        appClient3.send("shareLogin " + getUserName() + " " + token2 + " Android 24");
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(5, APP_SYNC, b("1 vw 1 1"))));

        appClient2.send("hardware 1 vw 2 2");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, APP_SYNC, b("1 vw 2 2"))));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();
        appClient2.reset();

        appClient2.send("hardware 1 ar 30");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(1, HARDWARE, b("ar 30"))));

        clientPair.appClient.send("hardware 1 ar 30");
        verify(appClient2.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(2, HARDWARE, b("ar 30"))));

        clientPair.hardwareClient.reset();
        clientPair.hardwareClient.send("ping");

        appClient2.send("hardware 1 pm 2 2");
        verify(clientPair.appClient.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock,  after(500).atMost(1)).channelRead(any(), any());

        clientPair.appClient.send("hardware 1 ar 30");
        verify(appClient2.responseMock, after(500).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(500).never()).channelRead(any(), eq(produce(2, HARDWARE, b("ar 30"))));

        clientPair.appClient.send("hardware 1 pm 2 2");
        verify(appClient2.responseMock, after(250).never()).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, after(250).never()).channelRead(any(), eq(produce(3, HARDWARE, b("pm 2 2"))));
    }

    @Test
    public void testSharingChargingCorrect() throws Exception {
        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, GET_ENERGY, "7500")));
        clientPair.appClient.reset();

        clientPair.appClient.send("getShareToken 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "6500")));

        clientPair.appClient.reset();

        clientPair.appClient.send("getShareToken 1");
        token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "6500")));
        clientPair.appClient.reset();

        clientPair.appClient.send("getShareToken 1");
        token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getShareToken 1");
        clientPair.appClient.send("getShareToken 1");
        clientPair.appClient.send("getShareToken 1");

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, GET_ENERGY, "6500")));
    }

    @Test
    public void checkStateWasChanged() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");
        appClient2.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw 1 1");
        appClient2.verifyResult(produce(2, APP_SYNC, b("1 vw 1 1")));

        appClient2.send("hardware 1 vw 2 2");
        clientPair.appClient.verifyResult(produce(2, APP_SYNC, b("1 vw 2 2")));

        clientPair.appClient.reset();
        appClient2.reset();

        //check from master side
        clientPair.appClient.send("hardware 1 aw 3 1");
        clientPair.hardwareClient.verifyResult(produce(1, HARDWARE, b("aw 3 1")));

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

        OnePinWidget tmp = getWidgetByPin(profile, 3);

        assertNotNull(tmp);
        assertEquals("1", tmp.value);


        //check from slave side
        appClient2.send("hardware 1 aw 3 150");
        clientPair.hardwareClient.verifyResult(produce(1, HARDWARE, b("aw 3 150")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);

        tmp = getWidgetByPin(profile, 3);

        assertNotNull(tmp);
        assertEquals("150", tmp.value);

        //check from hard side
        clientPair.hardwareClient.send("hardware aw 3 151");
        clientPair.appClient.verifyResult(produce(1, HARDWARE, b("1-0 aw 3 151")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        profile = clientPair.appClient.parseProfile(1);

        tmp = getWidgetByPin(profile, 3);

        assertNotNull(tmp);
        assertEquals("151", tmp.value);
    }

    @Test
    public void checkSetPropertyWasChanged() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");
        appClient2.verifyResult(ok(1));

        clientPair.hardwareClient.send("setProperty 1 color 123");
        clientPair.appClient.verifyResult(setProperty(1, "1-0 1 color 123"));
        appClient2.verifyResult(setProperty(1, "1-0 1 color 123"));
    }

    @Test
    public void checkSharingMessageWasReceived() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("sharing 1 off");
        clientPair.appClient.verifyResult(ok(2));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, SHARING, b("1 off"))));
    }

    @Test
    public void checkSharingMessageWasReceivedMultipleRecievers() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TestAppClient appClient3 = new TestAppClient(properties);
        appClient3.start();
        appClient3.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("sharing 1 off");
        clientPair.appClient.verifyResult(ok(2));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, SHARING, b("1 off"))));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(2, SHARING, b("1 off"))));
    }

    @Test
    public void checkSharingMessageWasReceivedAlsoForNonSharedApp() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        TestAppClient appClient3 = new TestAppClient(properties);
        appClient3.start();
        appClient3.login(getUserName(), "1", "Android", "1.10.4");

        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("sharing 1 off");
        clientPair.appClient.verifyResult(ok(2));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, SHARING, b("1 off"))));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(2, SHARING, b("1 off"))));
    }

    @Test
    public void eventorWorksInSharedModeFromAppSide() throws Exception {
        DataStream triggerDataStream = new DataStream((short) 1, PinType.VIRTUAL);
        DataStream dataStream = new DataStream((short) 2, PinType.VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "123", SetPinActionType.CUSTOM);
        Rule rule = new Rule(triggerDataStream, null, new GreaterThan(37), new BaseAction[] {setPinAction}, true);

        Eventor eventor = new Eventor();
        eventor.rules = new Rule[] {
                rule
        };

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody(2);
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient2.send("hardware 1 vw 1 38");

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 1 38"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, APP_SYNC, b("1 vw 1 38"))));

        clientPair.hardwareClient.verifyResult(hardware(888, "vw 2 123"));
        clientPair.appClient.verifyResult(hardware(888, "1-0 vw 2 123"));
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("1-0 vw 2 123"))));
    }

    @Test
    public void checkBothClientsReceiveMessage() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        //check from hard side
        clientPair.hardwareClient.send("hardware aw 3 151");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 aw 3 151"))));
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 aw 3 151"))));

        clientPair.hardwareClient.send("hardware aw 3 152");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 aw 3 152"))));
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 aw 3 152"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

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

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token+"a" + " Android 24");

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(notAllowed(1)));
    }

    @Test
    public void revokeSharedToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();
        appClient2.reset();

        assertFalse(clientPair.appClient.isClosed());
        assertFalse(appClient2.isClosed());

        clientPair.appClient.send("refreshShareToken 1");
        token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(notAllowed(1)));

        assertFalse(clientPair.appClient.isClosed());
        assertTrue(appClient2.isClosed());

        TestAppClient appClient3 = new TestAppClient(properties);
        appClient3.start();
        appClient3.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient3.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
    }

    @Test
    public void testDeactivateAndActivateForSubscriptions() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        TestAppClient appClient3 = new TestAppClient(properties);
        appClient3.start();
        appClient3.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(2));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "1")));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "1")));

        clientPair.appClient.activate(1);
        clientPair.appClient.verifyResult(ok(3));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(3, ACTIVATE_DASHBOARD, "1")));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(3, ACTIVATE_DASHBOARD, "1")));
    }

    @Test
    public void testDeactivateOnLogout() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        TestAppClient appClient3 = new TestAppClient(properties);
        appClient3.start();
        appClient3.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("deactivate");
        clientPair.appClient.verifyResult(ok(2));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "")));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "")));
    }

    @Test
    public void loadGzippedProfileForSharedBoard() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        String parentProfileString = clientPair.appClient.getBody();
        Profile parentProfile = JsonParser.parseProfileFromString(parentProfileString);

        appClient2.send("loadProfileGzipped");
        String body2 = appClient2.getBody(2);

        Twitter twitter = parentProfile.dashBoards[0].getTwitterWidget();
        clearPrivateData(twitter);
        Notification notification = parentProfile.dashBoards[0].getNotificationWidget();
        clearPrivateData(notification);
        for (Device device : parentProfile.dashBoards[0].devices) {
            device.token = null;
            device.hardwareInfo = null;
            device.deviceOtaInfo = null;
            device.lastLoggedIP = null;
            device.disconnectTime = 0;
            device.firstConnectTime = 0;
            device.dataReceivedAt = 0;
            device.connectTime = 0;
            device.status = null;
        }
        parentProfile.dashBoards[0].sharedToken = null;

        assertEquals(parentProfile.toString()
                        .replace("\"disconnectTime\":0,", "")
                        .replace("\"firstConnectTime\":0,", "")
                        .replace("\"dataReceivedAt\":0,", "")
                        .replace("\"connectTime\":0,", ""),
                body2);
    }

    @Test
    public void loadGzippedDashForSharedBoard() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        String parentProfileString = clientPair.appClient.getBody();
        Profile parentProfile = JsonParser.parseProfileFromString(parentProfileString);

        appClient2.send("loadProfileGzipped 1");
        String body2 = appClient2.getBody(2);

        Twitter twitter = parentProfile.dashBoards[0].getTwitterWidget();
        clearPrivateData(twitter);
        Notification notification = parentProfile.dashBoards[0].getNotificationWidget();
        clearPrivateData(notification);
        for (Device device : parentProfile.dashBoards[0].devices) {
            device.token = null;
            device.hardwareInfo = null;
            device.deviceOtaInfo = null;
            device.lastLoggedIP = null;
            device.disconnectTime = 0;
            device.firstConnectTime = 0;
            device.dataReceivedAt = 0;
            device.connectTime = 0;
            device.status = null;
        }
        parentProfile.dashBoards[0].sharedToken = null;

        assertEquals(parentProfile.dashBoards[0].toString()
                        .replace("\"disconnectTime\":0,", "")
                        .replace("\"firstConnectTime\":0,", "")
                        .replace("\"dataReceivedAt\":0,", "")
                        .replace("\"connectTime\":0,", ""),
                body2);
    }


    public static byte[] compress(String value) throws IOException {
        byte[] stringData = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(stringData.length);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            out.write(stringData);
        }

        return baos.toByteArray();
    }

    @Test
    public void testGetShareTokenAndRefresh() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();

        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient2.send("loadProfileGzipped");
        Profile serverProfile = appClient2.parseProfile(2);
        DashBoard dashboard = serverProfile.dashBoards[0];

        assertNotNull(dashboard);

        clientPair.appClient.reset();
        clientPair.appClient.send("refreshShareToken 1");
        String refreshedToken = clientPair.appClient.getBody();
        assertNotNull(refreshedToken);
        assertNotEquals(refreshedToken, token);

        TestAppClient appClient3 = new TestAppClient(properties);
        appClient3.start();
        appClient3.send("shareLogin " + getUserName() + " " + token + " Android 24");
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(notAllowed(1)));

        TestAppClient appClient4 = new TestAppClient(properties);
        appClient4.start();
        appClient4.send("shareLogin " + getUserName() + " " + refreshedToken + " Android 24");
        verify(appClient4.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient4.send("loadProfileGzipped");
        serverProfile = appClient4.parseProfile(2);
        DashBoard serverDash = serverProfile.dashBoards[0];

        assertNotNull(dashboard);
        Profile profile = parseProfile(readTestUserProfile());
        Twitter twitter = profile.dashBoards[0].getTwitterWidget();
        clearPrivateData(twitter);
        Notification notification = profile.dashBoards[0].getNotificationWidget();
        clearPrivateData(notification);

        //one field update, cause it is hard to compare.
        profile.dashBoards[0].updatedAt = serverDash.updatedAt;
        assertNull(serverDash.sharedToken);

        serverDash.devices = null;
        profile.dashBoards[0].devices = null;

        assertEquals(profile.dashBoards[0].toString(), serverDash.toString());
        //System.out.println(dashboard);
    }

    @Test
    public void testMasterMasterSyncWorksWithoutToken() throws Exception {
        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.login(getUserName(), "1", "Android", "24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, APP_SYNC, b("1 vw 1 1"))));

        appClient2.send("hardware 1 vw 2 2");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, APP_SYNC, b("1 vw 2 2"))));
    }

    @Test
    public void checkLogoutCommandForSharedApp() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());


        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, APP_SYNC, b("1 vw 1 1"))));

        clientPair.appClient.reset();
        clientPair.hardwareClient.reset();
        appClient2.reset();

        appClient2.send("addPushToken 1\0uid2\0token2");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        appClient2.send("logout uid2");
        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testSharedProjectDoesntReceiveCommandFromOtherProjects() throws Exception {
        DashBoard dash = new DashBoard();
        dash.id = 333;
        dash.name = "AAAa";
        dash.isShared = true;
        Device device = new Device();
        device.id = 0;
        device.name = "123";
        dash.devices = new Device[] {device};

        clientPair.appClient.createDash(dash);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("getShareToken 333");
        String token = clientPair.appClient.getBody(2);
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(properties);
        appClient2.start();
        appClient2.send("shareLogin " + getUserName() + " " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 1 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 1 1"))));
        verify(appClient2.responseMock, never()).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 1 1"))));
    }

    private static void clearPrivateData(Notification n) {
        n.iOSTokens.clear();
        n.androidTokens.clear();
    }

    private static void clearPrivateData(Twitter t) {
        t.username = null;
        t.token = null;
        t.secret = null;
    }

}
