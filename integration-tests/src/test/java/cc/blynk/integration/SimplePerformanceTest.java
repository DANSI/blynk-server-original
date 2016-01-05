package cc.blynk.integration;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.integration.model.ClientPair;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.integration.model.TestAppClient;
import cc.blynk.integration.model.TestHardClient;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.hardware.HardwareServer;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SimplePerformanceTest extends IntegrationBase {

    private NioEventLoopGroup sharedNioEventLoopGroup;

    private AppServer appServer;
    private HardwareServer hardwareServer;

    @Before
    public void init() throws Exception {
        this.sharedNioEventLoopGroup = new NioEventLoopGroup();

        initServerStructures();

        FileUtils.deleteDirectory(holder.fileManager.getDataDir().toFile());

        hardwareServer = new HardwareServer(holder);
        appServer = new AppServer(holder);
        hardwareServer.start();
        appServer.start();
        //wait util server starts.
        sleep(500);
    }


    @After
    public void shutdown() {
        appServer.stop();
        hardwareServer.stop();
    }

    @Test
    @Ignore
    public void emulateSlider() throws Exception {
        TestAppClient appClient = new TestAppClient("localhost", 8443);
        appClient.start(null);

        appClient.send("login dima@dima.ua 1");

        verify(appClient.responseMock, timeout(500)).channelRead(any(), any());

        for (int i = 0; i < 255; i++) {
            appClient.send("hardware aw 9 " + i);
            sleep(5);
        }
    }

    @Test
    public void testConnectAppAndHardware() throws Exception {
        int clientNumber = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        ClientPair[] clients = new ClientPair[clientNumber];
        List<Future<ClientPair>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();
        for (int i = 0; i < clientNumber; i++) {
            String usernameAndPass = "dima" + i +  "@mail.ua 1";

            Future<ClientPair> future = executorService.submit(
                    () -> initClientsWithSharedNio("localhost", appPort, hardPort, usernameAndPass, null, properties)
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

        /*
        System.currentTimeMillis();
        while (true) {
            for (ClientPair clientPair : clients) {
                clientPair.appClient.send("hardware aw 10 10");
                clientPair.hardwareClient.send("hardware vw 11 11");
            }
            sleep(10);
        }
        */
    }


    ClientPair initClientsWithSharedNio(String host, int appPort, int hardPort, String user, String jsonProfile,
                                  ServerProperties properties) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties, sharedNioEventLoopGroup);
        TestHardClient hardClient = new TestHardClient(host, hardPort, sharedNioEventLoopGroup);

        return initAppAndHardPair(appClient, hardClient, user, jsonProfile);
    }

}
