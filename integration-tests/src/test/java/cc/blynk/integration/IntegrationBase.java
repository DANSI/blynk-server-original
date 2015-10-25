package cc.blynk.integration;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.integration.model.TestAppClient;
import cc.blynk.integration.model.TestHardClient;
import cc.blynk.server.Holder;
import cc.blynk.server.model.Profile;
import cc.blynk.server.utils.JsonParser;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.List;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/4/2015.
 */
public abstract class IntegrationBase {

    static int appPort;
    public ServerProperties properties;
    public int hardPort;
    public String host;
    @Mock
    public BufferedReader bufferedReader;

    @Mock
    public BufferedReader bufferedReader2;

    @Mock
    public BlockingIOProcessor blockingIOProcessor;

    public Holder holder;

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //we can ignore it
        }

    }

    static String readTestUserProfile(String fileName) {
        if (fileName == null) {
            fileName = "user_profile_json.txt";
        }
        InputStream is = IntegrationBase.class.getResourceAsStream("/json_test/" + fileName);
        Profile profile = JsonParser.parseProfile(is);
        return profile.toString();
    }

    static String readTestUserProfile() {
        return readTestUserProfile(null);
    }

    static String getBody(SimpleClientHandler responseMock) throws Exception {
        ArgumentCaptor<Message> objectArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<Message> arguments = objectArgumentCaptor.getAllValues();
        Message getTokenMessage = arguments.get(0);
        return getTokenMessage.body;
    }

    @Before
    public void initBase() {
        properties = new ServerProperties();
        appPort = properties.getIntProperty("app.ssl.port");
        hardPort = properties.getIntProperty("hardware.default.port");
        host = "localhost";
    }

    ClientPair initAppAndHardPairNewAPI() throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", null, properties, true);
    }

    ClientPair initAppAndHardPair() throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", null, properties, false);
    }

    ClientPair initMutualAppAndHardPair(ServerProperties properties) throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "andrew@mail.ua 1", null, properties, false);
    }

    public ClientPair initAppAndHardPair(String jsonProfile) throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", jsonProfile, properties, false);
    }

    public ClientPair initAppAndHardPairNewAPI(String jsonProfile) throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", jsonProfile, properties, true);
    }

    ClientPair initAppAndHardPair(String host, int appPort, int hardPort, String user, String jsonProfile,
                                  ServerProperties properties, boolean newAPI) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties);

        TestHardClient hardClient = new TestHardClient(host, hardPort);

        appClient.start(null);
        hardClient.start(null);

        String userProfileString = readTestUserProfile(jsonProfile);
        int dashId = JsonParser.parseProfile(userProfileString, 1).dashBoards[0].id;

        appClient.send("register " + user)
                 .send("login " + user + (newAPI ? " Android 1RC7" : ""))
                 .send("saveProfile " + userProfileString)
                 .send("activate " + dashId)
                 .send("getToken " + dashId);

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(appClient.responseMock, timeout(2000).times(5)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        Message getTokenMessage = (Message) arguments.get(4);
        String token = getTokenMessage.body;

        hardClient.send("login " + token);
        verify(hardClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(1, OK)));

        appClient.reset();
        hardClient.reset();

        return new ClientPair(appClient, hardClient, token);
    }

    public void initServerStructures() {
        holder = new Holder(properties);
        holder.setBlockingIOProcessor(blockingIOProcessor);
    }
}
