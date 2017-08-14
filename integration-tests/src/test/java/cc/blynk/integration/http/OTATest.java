package cc.blynk.integration.http;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.hardware.HardwareServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.integration.IntegrationBase.b;
import static cc.blynk.integration.IntegrationBase.initAppAndHardPair;
import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class OTATest extends BaseTest {

    private BaseServer httpServer;
    private BaseServer hardwareServer;
    private BaseServer appServer;

    private CloseableHttpClient httpclient;
    private String httpServerUrl;

    private ClientPair clientPair;

    @After
    public void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
        hardwareServer.close();
        appServer.close();
        clientPair.stop();
    }

    @Before
    public void init() throws Exception {
        httpServer = new HttpAPIServer(holder, false).start();
        hardwareServer = new HardwareServer(holder).start();
        appServer = new AppServer(holder).start();
        httpServerUrl = String.format("http://localhost:%s/", httpPort);
        httpclient = HttpClients.createDefault();
        clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
        clientPair.hardwareClient.reset();
        clientPair.appClient.reset();
    }

    @Test
    public void testInitiateOTA() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/ota/start");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("", consumeText(response));
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7777, BLYNK_INTERNAL, b("ota http://127.0.0.1/static/ota/firmware_ota.bin"))));
    }

    @Test
    public void testInitiateOTAWithFileName() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/ota/start?fileName=test.bin");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("", consumeText(response));
        }

        String expectedResult = "http://127.0.0.1/static/ota/test.bin";
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7777, BLYNK_INTERNAL, b("ota " + expectedResult))));

        request = new HttpGet(httpServerUrl + token + "/ota/start?fileName=test.bin");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("", consumeText(response));
        }
    }

}
