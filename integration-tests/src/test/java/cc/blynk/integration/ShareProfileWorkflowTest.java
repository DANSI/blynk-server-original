package cc.blynk.integration;

import cc.blynk.common.model.messages.Message;
import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.server.TransportTypeHolder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static cc.blynk.common.enums.Response.NOT_ALLOWED;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    private static String getBody(SimpleClientHandler responseMock) throws Exception {
        ArgumentCaptor<Message> objectArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<Message> arguments = objectArgumentCaptor.getAllValues();
        Message getTokenMessage = arguments.get(0);
        return getTokenMessage.body;
    }

    @Before
    public void init() throws Exception {
        initServerStructures();

        FileUtils.deleteDirectory(fileManager.getDataDir().toFile());

        hardwareServer = new HardwareServer(properties, userRegistry, sessionsHolder, stats, notificationsProcessor, new TransportTypeHolder(properties), storageDao);
        appServer = new AppServer(properties, userRegistry, sessionsHolder, stats, new TransportTypeHolder(properties), storageDao);
        new Thread(hardwareServer).start();
        new Thread(appServer).start();

        //todo improve this
        //wait util server starts.
        sleep(500);

        clientPair = initAppAndHardPair();
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

        ClientPair clientPair2 = initAppAndHardPair("localhost", appPort, hardPort, "dima2@mail.ua 1", "user_profile_json_2.txt", properties);
        clientPair2.appClient.send("getSharedDash " + token);

        String dashboard = getBody(clientPair2.appClient.responseMock);

        assertNotNull(dashboard);
        Profile profile = JsonParser.parseProfile(readTestUserProfile(), 1);
        Twitter twitter = profile.dashBoards[0].getWidgetByType(Twitter.class);
        twitter.cleanPrivateData();
        Notification notification = profile.dashBoards[0].getWidgetByType(Notification.class);
        notification.cleanPrivateData();
        assertEquals(profile.dashBoards[0].toString(), dashboard);
        System.out.println(dashboard);
    }

}
