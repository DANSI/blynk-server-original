package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.ConnectRedirectMessage;
import cc.blynk.server.hardware.HardwareServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.OK;
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
public class LoadBalancingTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;

    @Before
    public void init() throws Exception {
        holder.props.setProperty("load.balancing.ips", "127.0.0.1");
        hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
        appServer = new AppServer(holder).start(transportTypeHolder);
        holder.redisClient.getClient().flushDB();
    }

    @After
    public void shutdown() {
        appServer.close();
        hardwareServer.close();
    }

    @Test
    public void testCreateFewAccountWithDifferentApp() throws Exception {
        TestAppClient appClient1 = new TestAppClient("localhost", tcpAppPort, properties);
        appClient1.start();

        String username = "test@gmmail.com";
        String pass = "a";
        String appName = "Blynk";

        appClient1.send("getServer");
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, ILLEGAL_COMMAND)));

        appClient1.send("getServer test@gmmail.com");
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new GetServerMessage(2, "127.0.0.1")));

        appClient1.send("register " + username + " " + pass + " " + appName);
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(3, OK)));
        appClient1.send("login " + username + " " + pass + " Android 1.10.4 " + appName);
        //we should wait until login finished. Only after that we can send commands
        verify(appClient1.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        appClient1.send("getServer test@gmmail.com");
        verify(appClient1.responseMock, timeout(1000).times(0)).channelRead(any(), eq(new GetServerMessage(5, "127.0.0.1")));
    }

    @Test
    public void testConnectRedirect() throws Exception {
        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        hardClient.start();

        hardClient.send("login 123");
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ResponseMessage(1, INVALID_TOKEN)));

        holder.redisClient.setServerByToken("123", "123.123.123.123");
        hardClient.send("login 123");
        verify(hardClient.responseMock, timeout(1000)).channelRead(any(), eq(new ConnectRedirectMessage(2, "123.123.123.123")));

    }

}
