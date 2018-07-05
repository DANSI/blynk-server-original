package cc.blynk.integration.tcp;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.properties.ServerProperties;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SimplePerformanceTest extends BaseTest {

    private NioEventLoopGroup sharedNioEventLoopGroup;

    private BaseServer appServer;
    private BaseServer hardwareServer;

    @Before
    public void init() throws Exception {
        this.sharedNioEventLoopGroup = new NioEventLoopGroup();
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new AppAndHttpsServer(holder).start();
    }


    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
    }

    @Test
    @Ignore
    public void testConnectAppAndHardware() throws Exception {
        int clientNumber = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        ClientPair[] clients = new ClientPair[clientNumber];
        List<Future<ClientPair>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();
        for (int i = 0; i < clientNumber; i++) {
            String email = "dima" + i +  "@mail.ua";

            Future<ClientPair> future = executorService.submit(
                    () -> initClientsWithSharedNio("localhost", properties.getHttpsPort(), tcpHardPort, email, "1", null, properties)
            );
            futures.add(future);
        }

        int counter = 0;
        for (Future<ClientPair> clientPairFuture : futures) {
            clients[counter] = clientPairFuture.get();
            //removing mocks, replace with real class
            clients[counter].appClient.replace(new SimpleClientHandler());
            clients[counter].hardwareClient.replace(new SimpleClientHandler());
            counter++;
        }

        System.out.println(clientNumber + " client pairs created in " + (System.currentTimeMillis() - start));
        assertEquals(clientNumber, counter);
    }


    ClientPair initClientsWithSharedNio(String host, int appPort, int hardPort, String user, String pass, String jsonProfile,
                                        ServerProperties properties) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties, sharedNioEventLoopGroup);
        TestHardClient hardClient = new TestHardClient(host, hardPort, sharedNioEventLoopGroup);

        return TestUtil.initAppAndHardPair(appClient, hardClient, user, pass, jsonProfile, 10000);
    }

}
