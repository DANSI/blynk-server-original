package cc.blynk.integration.tcp;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.properties.ServerProperties;
import cc.blynk.utils.structure.LRUCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static cc.blynk.integration.TestUtil.connectRedirect;
import static cc.blynk.integration.TestUtil.createDefaultHolder;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.getServer;
import static cc.blynk.integration.TestUtil.illegalCommand;
import static cc.blynk.integration.TestUtil.invalidToken;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 5/09/2016.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadBalancingIntegrationTest extends BaseTest {

    private BaseServer appServer1;
    private BaseServer hardwareServer1;
    private Holder holder;

    private BaseServer appServer2;
    private BaseServer hardwareServer2;
    private int tcpAppPort2;
    private int plainHardPort2;
    private Holder holder2;
    private ServerProperties properties2;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        holder = createDefaultHolder(properties, "db-test.properties");;
        hardwareServer1 = new HardwareAndHttpAPIServer(holder).start();
        appServer1 = new MobileAndHttpsServer(holder).start();

        properties2 = new ServerProperties(Collections.emptyMap(), "server2.properties");
        properties2.setProperty("data.folder", getDataFolder());

        this.holder2 = createDefaultHolder(properties2, "db-test.properties");;
        hardwareServer2 = new HardwareAndHttpAPIServer(holder2).start();
        appServer2 = new MobileAndHttpsServer(holder2).start();
        plainHardPort2 = properties2.getIntProperty("http.port");
        tcpAppPort2 = properties2.getIntProperty("https.port");

        holder.dbManager.executeSQL("DELETE FROM users");
        holder.dbManager.executeSQL("DELETE FROM forwarding_tokens");
        clientPair = initAppAndHardPair(properties.getHttpsPort(), properties.getHttpPort(), properties2);
    }

    @After
    public void shutdown() {
        appServer1.close();
        hardwareServer1.close();

        appServer2.close();
        hardwareServer2.close();

        holder.close();
        holder2.close();

        clientPair.stop();
    }

    @Test
    @Ignore
    //todo fix, local DB was changed, need fix
    public void test2NewUsersStoredOnDifferentServers() throws Exception {
        TestAppClient appClient1 =new TestAppClient(properties);
        appClient1.start();

        String email = "test_new@gmail.com";
        String pass = "a";
        String appName = AppNameUtil.BLYNK;

        appClient1.send("getServer " + email + "\0" + appName);
        appClient1.verifyResult(getServer(1, "127.0.0.1"));

        appClient1.reset();

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(holder.userDao, holder.fileManager, holder.dbManager);
        ProfileSaverWorker profileSaverWorker2 = new ProfileSaverWorker(holder2.userDao, holder2.fileManager, holder2.dbManager);

        workflowForUser(appClient1, email, pass, appName);
        profileSaverWorker.run();
        //waiting for DB update
        TestUtil.sleep(500);

        assertEquals("127.0.0.1", holder.dbManager.getUserServerIp(email, AppNameUtil.BLYNK));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort2, properties2);
        appClient2.start();

        String username2 = "test2_new@gmail.com";

        appClient2.send("getServer " + username2 + "\0" + appName);
        appClient2.verifyResult(getServer(1, "localhost2"));

        appClient2.reset();

        workflowForUser(appClient2, username2, pass, appName);
        profileSaverWorker2.run();

        long tries = 0;
        String host;
        //waiting for channel to be closed.
        //but only limited amount if time
        while ((host = holder2.dbManager.getUserServerIp(username2, AppNameUtil.BLYNK)) == null && tries < 100) {
            TestUtil.sleep(10);
            tries++;
        }

        assertEquals("localhost2", host);
    }

    @Test
    public void testNoGetServerHandlerAfterLogin() throws Exception {
        TestAppClient appClient1 =new TestAppClient(properties);
        appClient1.start();
        workflowForUser(appClient1, "123@gmail.com", "a", AppNameUtil.BLYNK);
        appClient1.send("getServer " + "123@gmail.com" + "\0" + AppNameUtil.BLYNK);
        appClient1.neverAfter(500, getServer(1, "127.0.0.1"));
    }

    @Test
    @Ignore
    //todo fix, local DB was changed, need fix
    public void testUserRedirectedToCorrectServer() throws Exception {
        TestAppClient appClient1 =new TestAppClient(properties);
        appClient1.start();

        String email = "test_new@gmail.com";
        String pass = "a";
        String appName = AppNameUtil.BLYNK;

        appClient1.send("getServer " + email + "\0" + appName);
        appClient1.verifyResult(getServer(1, "127.0.0.1"));

        appClient1.reset();

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(holder.userDao, holder.fileManager, holder.dbManager);

        workflowForUser(appClient1, email, pass, appName);
        profileSaverWorker.run();
        //waiting for DB update
        TestUtil.sleep(500);

        assertEquals("127.0.0.1", holder.dbManager.getUserServerIp(email, AppNameUtil.BLYNK));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort2, properties2);
        appClient2.start();

        appClient2.send("getServer " + email + "\0" + appName);
        appClient2.verifyResult(getServer(1, "127.0.0.1"));
    }

    @Test
    public void testCreateFewAccountWithDifferentApp() throws Exception {
        TestAppClient appClient1 = new TestAppClient(properties);
        appClient1.start();

        String email = "test@gmmail.com";
        String pass = "a";
        String appName = "Blynk";

        appClient1.send("getServer");
        appClient1.verifyResult(illegalCommand(1));

        appClient1.send("getServer " + email + "\0" + appName);
        appClient1.verifyResult(getServer(2, "127.0.0.1"));

        appClient1.register(email, pass, appName);
        appClient1.verifyResult(ok(3));
        appClient1.login(email, pass, "Android", "1.10.4 " + appName);
        //we should wait until login finished. Only after that we can send commands
        appClient1.verifyResult(ok(4));

        appClient1.send("getServer " + email + "\0" + appName);
        appClient1.never(getServer(5, "127.0.0.1"));
    }

    @Test
    public void testNoRedirectAsTokenIsWrong() throws Exception {
        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.login("123");
        hardClient.verifyResult(invalidToken(1));

        holder.dbManager.assignServerToToken("123", "127.0.0.1", "user", 0, 0);
        hardClient.login("123");
        hardClient.verifyResult(invalidToken(2));

        hardClient.login("\0");
        hardClient.verifyResult(invalidToken(3));
    }

    @Test
    public void hardwareCreatedAndServerStoredInDB() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResultAfter(500, createDevice(1, device));

        assertEquals("127.0.0.1", holder.dbManager.forwardingTokenDBDao.selectHostByToken(device.token));
    }

    @Test
    public void hardwareCreatedAndServerStoredInDBAndDelete() throws Exception {
        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        device1.status = Status.OFFLINE;

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();
        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResultAfter(1000, createDevice(1, device));

        assertEquals("127.0.0.1", holder.dbManager.forwardingTokenDBDao.selectHostByToken(device.token));

        clientPair.appClient.send("deleteDevice 1\0" + device.id);
        clientPair.appClient.verifyResultAfter(1000, ok(2));

        assertNull(holder.dbManager.forwardingTokenDBDao.selectHostByToken(device.token));
    }

    @Test
    public void redirectForHardwareWorks() throws Exception {
        String token = "12345678901234567890123456789012";

        assertTrue(holder.dbManager.forwardingTokenDBDao.insertTokenHost(
                token, "test_host", getUserName(), 0, 0));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.login(token);
        hardClient.verifyResult(connectRedirect(1, "test_host " + tcpHardPort));
    }

    @Test
    public void redirectForHardwareWorksWithForce80Port() throws Exception {
        String token = "12345678901234567890123456789013";

        assertTrue(holder.dbManager.forwardingTokenDBDao.insertTokenHost(
                token, "test_host", getUserName(), 0, 0));

        TestHardClient hardClient = new TestHardClient("localhost", plainHardPort2);
        hardClient.start();

        hardClient.login(token);
        hardClient.verifyResult(connectRedirect(1, "test_host " + 80));
    }

    @Test
    public void testInvalidToken() throws Exception {
        String token = "1234567890123456789012345678901";

        assertTrue(holder.dbManager.forwardingTokenDBDao.insertTokenHost(
                token, "test_host", getUserName(), 0, 0));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.login(token);
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(invalidToken(1)));
    }

    @Test
    public void redirectForHardwareWorksFromCache() throws Exception {
        String token = "12345678901234567890123456789013";

        assertTrue(holder.dbManager.forwardingTokenDBDao.insertTokenHost(
                token, "test_host", getUserName(), 0, 0));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.login(token);
        hardClient.verifyResult(connectRedirect(1, "test_host " + tcpHardPort));

        holder.dbManager.executeSQL("DELETE FROM forwarding_tokens");

        hardClient.login(token);
        hardClient.verifyResult(connectRedirect(2, "test_host " + tcpHardPort));
    }

    @Test
    public void redirectForHardwareDoesntWorkFromInvalidatedCache() throws Exception {
        String token = "12345678901234567890123456789012";

        assertTrue(holder.dbManager.forwardingTokenDBDao.insertTokenHost(
                token, "test_host", getUserName(), 0, 0));

        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.login(token);
        hardClient.verifyResult(connectRedirect(1, "test_host " + tcpHardPort));

        holder.dbManager.executeSQL("DELETE FROM forwarding_tokens");

        hardClient.login(token);
        hardClient.verifyResult(connectRedirect(2, "test_host " + tcpHardPort));

        LRUCache.LOGIN_TOKENS_CACHE.clear();

        hardClient.login(token);
        hardClient.verifyResult(invalidToken(3));

        assertTrue(holder.dbManager.forwardingTokenDBDao.insertTokenHost(
                token, "test_host_2", getUserName(), 0, 0));

        LRUCache.LOGIN_TOKENS_CACHE.clear();

        hardClient.login(token);
        hardClient.verifyResult(connectRedirect(4, "test_host_2 " + tcpHardPort));
    }

    private String workflowForUser(TestAppClient appClient, String username, String pass, String appName) throws Exception{
        appClient.register(username,  pass, appName);
        appClient.verifyResult(ok(1));
        appClient.login(username, pass, "Android", "1.10.4 " + appName);
        appClient.verifyResult(ok(2));

        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "test";
        appClient.createDash(dash);
        appClient.verifyResult(ok(3));
        appClient.activate(1);
        appClient.verifyResult(new ResponseMessage(4, DEVICE_NOT_IN_NETWORK));

        appClient.reset();
        appClient.createDevice(1, new Device(0, "123", BoardType.ESP8266));
        Device device = appClient.parseDevice();

        String token = device.token;
        assertNotNull(token);
        return token;
    }

}
