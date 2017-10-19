package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.Holder;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.properties.ServerProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Response.DEVICE_NOT_IN_NETWORK;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 5/09/2016.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadBalancingIntegrationTest extends IntegrationBase {

    private BaseServer appServer1;
    private BaseServer hardwareServer1;
    private Holder holder;

    private BaseServer appServer2;
    private BaseServer hardwareServer2;
    private int tcpAppPort2;
    private int plainHardPort2;
    private Holder holder2;
    private ServerProperties properties2;

    @Before
    public void init() throws Exception {
        holder = new Holder(properties, twitterWrapper, mailWrapper, gcmWrapper, smsWrapper, "db-test.properties");
        hardwareServer1 = new HardwareServer(holder).start();
        appServer1 = new AppServer(holder).start();

        properties2 = new ServerProperties("server2.properties");
        properties2.setProperty("data.folder", getDataFolder());

        this.holder2 = new Holder(properties2, twitterWrapper, mailWrapper, gcmWrapper, smsWrapper, "db-test.properties");
        hardwareServer2 = new HardwareServer(holder2).start();
        appServer2 = new AppServer(holder2).start();
        plainHardPort2 = properties2.getIntProperty("hardware.default.port");
        tcpAppPort2 = properties2.getIntProperty("app.ssl.port");

        holder.dbManager.executeSQL("DELETE FROM users");
    }

    @After
    public void shutdown() {
        appServer1.close();
        hardwareServer1.close();

        appServer2.close();
        hardwareServer2.close();

        holder.close();
        holder2.close();
    }

    @Test
    public void test2NewUsersStoredOnDifferentServers() throws Exception {
        TestAppClient appClient1 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient1.start();

        String email = "test_new@gmail.com";
        String pass = "a";
        String appName = AppNameUtil.BLYNK;

        appClient1.send("getServer " + email + "\0" + appName);
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new GetServerMessage(1, "127.0.0.1")));

        appClient1.reset();

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(holder.userDao, holder.fileManager, holder.dbManager);
        ProfileSaverWorker profileSaverWorker2 = new ProfileSaverWorker(holder2.userDao, holder2.fileManager, holder2.dbManager);

        workflowForUser(appClient1, email, pass, appName);
        profileSaverWorker.run();
        //waiting for DB update
        sleep(500);

        assertEquals("127.0.0.1", holder.dbManager.getUserServerIp(email, AppNameUtil.BLYNK));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort2, properties2);
        appClient2.start();

        String username2 = "test2_new@gmail.com";

        appClient2.send("getServer " + username2 + "\0" + appName);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(new GetServerMessage(1, "localhost2")));

        appClient2.reset();

        workflowForUser(appClient2, username2, pass, appName);
        profileSaverWorker2.run();
        //waiting for DB update
        sleep(500);

        assertEquals("localhost2", holder2.dbManager.getUserServerIp(username2, AppNameUtil.BLYNK));
    }

    @Test
    public void testNoGetServerHandlerAfterLogin() throws Exception {
        TestAppClient appClient1 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient1.start();
        workflowForUser(appClient1, "123@gmail.com", "a", AppNameUtil.BLYNK);
        appClient1.send("getServer " + "123@gmail.com" + "\0" + AppNameUtil.BLYNK);
        verify(appClient1.responseMock, after(500).never()).channelRead(any(), eq(new GetServerMessage(1, "127.0.0.1")));
    }

    @Test
    public void testUserRedirectedToCorrectServer() throws Exception {
        TestAppClient appClient1 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient1.start();

        String email = "test_new@gmail.com";
        String pass = "a";
        String appName = AppNameUtil.BLYNK;

        appClient1.send("getServer " + email + "\0" + appName);
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new GetServerMessage(1, "127.0.0.1")));

        appClient1.reset();

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(holder.userDao, holder.fileManager, holder.dbManager);

        workflowForUser(appClient1, email, pass, appName);
        profileSaverWorker.run();
        //waiting for DB update
        sleep(500);

        assertEquals("127.0.0.1", holder.dbManager.getUserServerIp(email, AppNameUtil.BLYNK));

        TestAppClient appClient2 = new TestAppClient("localhost", tcpAppPort2, properties2);
        appClient2.start();

        appClient2.send("getServer " + email + "\0" + appName);
        verify(appClient2.responseMock, timeout(1000)).channelRead(any(), eq(new GetServerMessage(1, "127.0.0.1")));
    }

    @Test
    public void testCreateFewAccountWithDifferentApp() throws Exception {
        TestAppClient appClient1 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient1.start();

        String email = "test@gmmail.com";
        String pass = "a";
        String appName = "Blynk";

        appClient1.send("getServer");
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        appClient1.send("getServer " + email + "\0" + appName);
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new GetServerMessage(2, "127.0.0.1")));

        appClient1.send("register " + email + " " + pass + " " + appName);
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        appClient1.send("login " + email + " " + pass + " Android 1.10.4 " + appName);
        //we should wait until login finished. Only after that we can send commands
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        appClient1.send("getServer " + email + "\0" + appName);
        verify(appClient1.responseMock, timeout(1000).times(0)).channelRead(any(), eq(new GetServerMessage(5, "127.0.0.1")));
    }

    @Test
    public void testNoRedirectAsTokenIsWrong() throws Exception {
        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.send("login 123");
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, INVALID_TOKEN)));

        holder.dbManager.assignServerToToken("123", "127.0.0.1");
        hardClient.send("login 123");
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(2, INVALID_TOKEN)));
    }

    private String workflowForUser(TestAppClient appClient, String username, String pass, String appName) throws Exception{
        appClient.send("register " + username + " " + pass + " " + appName);
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, OK)));
        appClient.send("login " + username + " " + pass + " Android 1.10.4 " + appName);
        //we should wait until login finished. Only after that we can send commands
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        DashBoard dash = new DashBoard();
        dash.id = 1;
        dash.name = "test";
        appClient.send("createDash " + dash.toString());
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        appClient.send("activate 1");
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(4, DEVICE_NOT_IN_NETWORK)));

        appClient.reset();
        appClient.send("getToken 1");

        String token = appClient.getBody();
        assertNotNull(token);
        return token;
    }

}
