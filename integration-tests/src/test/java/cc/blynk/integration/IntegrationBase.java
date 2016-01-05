package cc.blynk.integration;

import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.integration.model.TestAppClient;
import cc.blynk.integration.model.TestHardClient;
import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetTokenMessage;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ServerProperties;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage getTokenMessage = arguments.get(0);
        return getTokenMessage.body;
    }

    private static GetTokenMessage getGetTokenMessage(List<Object> arguments) {
        for (Object obj : arguments) {
            if (obj instanceof GetTokenMessage) {
                return (GetTokenMessage) obj;
            }
        }
        throw new RuntimeException("Get token message wasn't retrieved.");
    }

    public static String getProfileFolder() throws Exception {
        URL resource = IntegrationBase.class.getResource("/profiles");
        String resourcesPath = Paths.get(resource.toURI()).toAbsolutePath().toString();
        System.out.println("Resource path : " + resourcesPath);
        return resourcesPath;
    }

    @Before
    public void initBase() {
        properties = new ServerProperties();
        appPort = properties.getIntProperty("app.ssl.port");
        hardPort = properties.getIntProperty("hardware.default.port");
        host = "localhost";
    }

    ClientPair initAppAndHardPair() throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", null, properties);
    }

    public ClientPair initAppAndHardPair(String jsonProfile) throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", jsonProfile, properties);
    }

    ClientPair initAppAndHardPair(String host, int appPort, int hardPort, String user, String jsonProfile,
                                  ServerProperties properties) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties);
        TestHardClient hardClient = new TestHardClient(host, hardPort);

        return initAppAndHardPair(appClient, hardClient, user, jsonProfile);
    }

    ClientPair initAppAndHardPair(TestAppClient appClient, TestHardClient hardClient, String user,
                                  String jsonProfile) throws Exception {

        appClient.start(null);
        hardClient.start(null);

        String userProfileString = readTestUserProfile(jsonProfile);
        int dashId = JsonParser.parseProfile(userProfileString, 1).dashBoards[0].id;

        appClient.send("register " + user)
                 .send("login " + user + " Android 1RC7")
                 .send("saveProfile " + userProfileString)
                 .send("activate " + dashId)
                 .send("getToken " + dashId);

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(appClient.responseMock, timeout(2000).times(5)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token = getGetTokenMessage(arguments).body;

        hardClient.send("login " + token);
        verify(hardClient.responseMock, timeout(2000)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        appClient.reset();
        hardClient.reset();

        return new ClientPair(appClient, hardClient, token);
    }

    public void initServerStructures() {
        holder = new Holder(properties);
        holder.setBlockingIOProcessor(blockingIOProcessor);
    }
}
