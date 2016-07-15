package cc.blynk.integration.http;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.hardware.HardwareServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAndTCPSameJVMTest extends IntegrationBase {

    private static BaseServer httpServer;
    private static BaseServer hardwareServer;
    private static BaseServer appServer;

    private static CloseableHttpClient httpclient;
    private static String httpServerUrl;

    private static ClientPair clientPair;

    @AfterClass
    public static void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
        hardwareServer.close();
        appServer.close();
        clientPair.stop();
    }

    @Before
    public void init() throws Exception {
        if (httpServer == null) {
            httpServer = new HttpAPIServer(holder).start(transportTypeHolder);
            hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
            appServer = new AppServer(holder).start(transportTypeHolder);
            httpServerUrl = String.format("http://localhost:%s/", httpPort);
            httpclient = HttpClients.createDefault();
        }
        if (clientPair == null) {
            clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
        }
        clientPair.hardwareClient.reset();
        clientPair.appClient.reset();
    }

    @Override
    public String getDataFolder() {
        return IntegrationBase.getProfileFolder();
    }

    @Test
    public void testChangeNonWidgetPinValueViaHardwareAndGetViaHTTP() throws Exception {
        clientPair.hardwareClient.send("hardware vw 10 200");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 10 200"))));

        reset(clientPair.appClient.responseMock);

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/pin/v10");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("200", values.get(0));
        }

        clientPair.appClient.send("hardware 1 vw 10 201");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 10 201"))));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("201", values.get(0));
        }
    }

    @Test
    public void testChangePinValueViaAppAndHardware() throws Exception {
        clientPair.hardwareClient.send("hardware vw 4 200");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 4 200"))));

        reset(clientPair.appClient.responseMock);

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/pin/v4");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("200", values.get(0));
        }

        clientPair.appClient.send("hardware 1 vw 4 201");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 4 201"))));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("201", values.get(0));
        }
    }

    @Test
    public void testChangePinValueViaAppAndHardwareForWrongPWMButton() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"DIGITAL\",\"pin\":18,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"value\":\"1\",\"pushMode\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet requestGET = new HttpGet(httpServerUrl + token + "/pin/d18");

        try (CloseableHttpResponse response = httpclient.execute(requestGET)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("1", values.get(0));
        }

        HttpPut requestPUT = new HttpPut(httpServerUrl + token + "/pin/d18");
        requestPUT.setEntity(new StringEntity("[\"0\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(requestPUT)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("dw 18 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 dw 18 0"))));
    }

    @Test
    public void testChangePinValueViaHttpAPI() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v4");
        HttpGet getRequest = new HttpGet(httpServerUrl + token + "/pin/v4");

        for (int i = 0; i < 100; i++) {
            request.setEntity(new StringEntity("[\"" + i + "\"]", ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
            }

            clientPair.hardwareClient.send("hardsync " + b("vr 4"));
            verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(i + 1, HARDWARE, b("vw 4 " + i))));

            try (CloseableHttpResponse response = httpclient.execute(getRequest)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                List<String> values = consumeJsonPinValues(response);
                assertEquals(1, values.size());
                assertEquals(i, Integer.valueOf(values.get(0)).intValue());
            }
        }
    }

    @Test
    public void testIsHardwareAndAppConnected() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }

        request = new HttpGet(httpServerUrl + token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }
    }

    @Test
    public void testIsHardwareAndAppDisconnected() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        clientPair.stop();

        HttpGet request = new HttpGet(httpServerUrl + token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        request = new HttpGet(httpServerUrl + token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        clientPair = null;
    }

    @Test
    public void testChangePinValueViaHttpAPIAndNoActiveProject() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();
        clientPair.appClient.reset();

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v31");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100"))));
        verify(clientPair.appClient.responseMock, after(400).never()).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 31 100"))));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
    }

    @Test
    public void testChangeLCDPinValueViaHttpAPIAndValueChanged() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v0");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 0 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 0 100"))));

        request = new HttpPut(httpServerUrl + token + "/pin/v1");

        request.setEntity(new StringEntity("[\"101\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 1 101"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 1 101"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndNoWidgetSinglePinValue() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v31");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 31 100"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndForTerminal() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        clientPair.appClient.send("createWidget 1\0{\"id\":222, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":100}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/V100");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 100 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 100 100"))));
    }


    @Test
    public void testChangePinValueViaHttpAPIAndNoWidgetMultiPinValue() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v31");

        request.setEntity(new StringEntity("[\"100\",\"101\",\"102\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100 101 102"))));
    }

}
