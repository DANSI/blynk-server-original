package cc.blynk.integration;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.TestAppClient;
import cc.blynk.integration.model.TestHardClient;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.Profile;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.storage.reporting.average.AverageAggregator;
import cc.blynk.server.utils.JsonParser;
import cc.blynk.server.utils.ReportingUtil;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
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
    public NotificationsProcessor notificationsProcessor;

    public AverageAggregator averageAggregator;

    public StorageDao storageDao;

    public FileManager fileManager;

    public SessionsHolder sessionsHolder;

    public UserRegistry userRegistry;

    public GlobalStats stats;

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

    ClientPair initMutualAppAndHardPair(ServerProperties properties) throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "andrew@mail.ua 1", null, properties);
    }

    public ClientPair initAppAndHardPair(String jsonProfile) throws Exception {
        return initAppAndHardPair("localhost", appPort, hardPort, "dima@mail.ua 1", jsonProfile, properties);
    }

    ClientPair initAppAndHardPair(String host, int appPort, int hardPort, String user, String jsonProfile,
                                  ServerProperties properties) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties);

        TestHardClient hardClient = new TestHardClient(host, hardPort);

        appClient.start(null);
        hardClient.start(null);

        String userProfileString = readTestUserProfile(jsonProfile);

        appClient.send("register " + user)
                 .send("login " + user)
                 .send("saveProfile " + userProfileString)
                 .send("activate 1")
                 .send("getToken 1");

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
        String dataFolder = properties.getProperty("data.folder");
        fileManager = new FileManager(dataFolder);
        sessionsHolder = new SessionsHolder();
        userRegistry = new UserRegistry(fileManager.deserialize());
        stats = new GlobalStats();
        averageAggregator = new AverageAggregator(ReportingUtil.getReportingFolder(dataFolder));
        storageDao = new StorageDao(properties.getIntProperty("user.in.memory.storage.limit"), averageAggregator, System.getProperty("java.io.tmpdir"));
    }
}
