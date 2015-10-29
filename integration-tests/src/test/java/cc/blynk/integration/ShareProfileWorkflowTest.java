package cc.blynk.integration;

import cc.blynk.common.utils.StringUtils;
import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.TestAppClient;
import cc.blynk.server.core.application.AppServer;
import cc.blynk.server.core.hardware.HardwareServer;
import cc.blynk.server.model.Profile;
import cc.blynk.server.model.widgets.others.Notification;
import cc.blynk.server.model.widgets.others.Twitter;
import cc.blynk.server.utils.JsonParser;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.common.enums.Command.*;
import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;
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

    private AppServer appServer;
    private HardwareServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        initServerStructures();

        FileUtils.deleteDirectory(holder.fileManager.getDataDir().toFile());

        hardwareServer = new HardwareServer(holder);
        appServer = new AppServer(holder);
        new Thread(hardwareServer).start();
        new Thread(appServer).start();

        //todo improve this
        //wait util server starts.
        sleep(500);

        clientPair = initAppAndHardPairNewAPI();
    }

    @After
    public void shutdown() {
        appServer.stop();
        hardwareServer.stop();
        clientPair.stop();
    }

    @Test
    public void testGetShareTokenNoDashId() throws Exception {
        clientPair.appClient.send("getShareToken");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, NOT_ALLOWED)));
    }

    @Test
    public void testGetShareToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = getBody(clientPair.appClient.responseMock);
        assertNotNull(token);
        assertEquals(32, token.length());

        ClientPair clientPair2 = initAppAndHardPair("localhost", appPort, hardPort, "dima2@mail.ua 1", "user_profile_json_2.txt", properties, true);
        clientPair2.appClient.send("getSharedDash " + token);

        String dashboard = getBody(clientPair2.appClient.responseMock);

        assertNotNull(dashboard);
        Profile profile = JsonParser.parseProfile(readTestUserProfile(), 1);
        Twitter twitter = profile.dashBoards[0].getWidgetByType(Twitter.class);
        twitter.cleanPrivateData();
        Notification notification = profile.dashBoards[0].getWidgetByType(Notification.class);
        notification.cleanPrivateData();
        assertEquals(profile.dashBoards[0].toString(), dashboard);
        //System.out.println(dashboard);
    }

    @Test
    public void getShareTokenAndLoginViaIt() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = getBody(clientPair.appClient.responseMock);
        assertNotNull(token);
        assertEquals(32, token.length());


        TestAppClient appClient2 = new TestAppClient(host, appPort, properties);
        appClient2.start(null);
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.appClient.send("hardware 1 vw 1 1");
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, SYNC, "1 vw 1 1".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING))));

        appClient2.send("hardware 1 vw 2 2");
        verify(clientPair.appClient.responseMock, timeout(1000)).channelRead(any(), eq(produce(2, SYNC, "1 vw 2 2".replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING))));

        clientPair.appClient.reset();

        appClient2.send("hardware 1 vr 2 2");
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), any());

        appClient2.send("hardware 1 pm 2 2");
        verify(clientPair.appClient.responseMock, never()).channelRead(any(), any());
    }

    @Test
    public void wrongSharedToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = getBody(clientPair.appClient.responseMock);
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(host, appPort, properties);
        appClient2.start(null);
        appClient2.send("shareLogin " + "dima@mail.ua " + token+"a" + " Android 24");

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, NOT_ALLOWED)));
    }

    @Test
    public void revokeSharedToken() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = getBody(clientPair.appClient.responseMock);
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(host, appPort, properties);
        appClient2.start(null);
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));

        clientPair.appClient.reset();
        appClient2.reset();

        assertFalse(clientPair.appClient.isClosed());
        assertFalse(appClient2.isClosed());

        clientPair.appClient.send("refreshShareToken 1");
        token = getBody(clientPair.appClient.responseMock);
        assertNotNull(token);
        assertEquals(32, token.length());

        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, NOT_ALLOWED)));

        assertFalse(clientPair.appClient.isClosed());
        assertTrue(appClient2.isClosed());

        TestAppClient appClient3 = new TestAppClient(host, appPort, properties);
        appClient3.start(null);
        appClient3.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient3.responseMock, timeout(1000)).channelRead(any(), eq(produce(1, OK)));
    }

    @Test
    public void testDeactivateAndActivateForSubscriptions() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = getBody(clientPair.appClient.responseMock);
        assertNotNull(token);
        assertEquals(32, token.length());

        TestAppClient appClient2 = new TestAppClient(host, appPort, properties);
        appClient2.start(null);
        appClient2.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        TestAppClient appClient3 = new TestAppClient(host, appPort, properties);
        appClient3.start(null);
        appClient3.send("shareLogin " + "dima@mail.ua " + token + " Android 24");

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(1, OK)));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, OK)));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "1")));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(2, DEACTIVATE_DASHBOARD, "1")));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, OK)));

        verify(appClient2.responseMock, timeout(500)).channelRead(any(), eq(produce(3, ACTIVATE_DASHBOARD, "1")));
        verify(appClient3.responseMock, timeout(500)).channelRead(any(), eq(produce(3, ACTIVATE_DASHBOARD, "1")));
    }

    @Test
    public void testGetShareTokenAndRefresh() throws Exception {
        clientPair.appClient.send("getShareToken 1");

        String token = getBody(clientPair.appClient.responseMock);
        assertNotNull(token);
        assertEquals(32, token.length());

        ClientPair clientPair2 = initAppAndHardPair("localhost", appPort, hardPort, "dima2@mail.ua 1", "user_profile_json_2.txt", properties, true);
        clientPair2.appClient.send("getSharedDash " + token);

        String dashboard = getBody(clientPair2.appClient.responseMock);

        assertNotNull(dashboard);

        clientPair.appClient.reset();
        clientPair.appClient.send("refreshShareToken 1");
        String refreshedToken = getBody(clientPair.appClient.responseMock);
        assertNotNull(refreshedToken);
        assertNotEquals(refreshedToken, token);

        clientPair.appClient.reset();
        clientPair.appClient.send("getSharedDash " + token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, INVALID_TOKEN)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getSharedDash " + refreshedToken);

        dashboard = getBody(clientPair.appClient.responseMock);

        assertNotNull(dashboard);
        Profile profile = JsonParser.parseProfile(readTestUserProfile(), 1);
        Twitter twitter = profile.dashBoards[0].getWidgetByType(Twitter.class);
        twitter.cleanPrivateData();
        Notification notification = profile.dashBoards[0].getWidgetByType(Notification.class);
        notification.cleanPrivateData();
        assertEquals(profile.dashBoards[0].toString(), dashboard);
        //System.out.println(dashboard);
    }


}
