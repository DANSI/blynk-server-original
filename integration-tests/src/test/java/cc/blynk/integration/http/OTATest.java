package cc.blynk.integration.http;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.hardware.BlynkInternalMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static cc.blynk.integration.IntegrationBase.*;
import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.*;
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

    @Test
    public void testImprovedUploadMethod() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPost post = new HttpPost(httpServerUrl + token + "/ota/start");

        String fileName = "test.bin";

        InputStream binFile = OTATest.class.getResourceAsStream("/static/ota/" + fileName);
        ContentBody fileBody = new InputStreamBody(binFile, ContentType.APPLICATION_OCTET_STREAM, fileName);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("upfile", fileBody);
        HttpEntity entity = builder.build();

        post.setEntity(entity);

        String path;
        try (CloseableHttpResponse response = httpclient.execute(post)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            path = consumeText(response);

            assertNotNull(path);
            assertTrue(path.startsWith("/static"));
            assertTrue(path.endsWith("bin"));
        }

        String responseUrl = "http://127.0.0.1" + path;
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new BlynkInternalMessage(7777, b("ota " + responseUrl))));
    }

    @Test
    public void testOTAFailedWhenNoDevice() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        clientPair.hardwareClient.stop();

        HttpPost post = new HttpPost(httpServerUrl + token + "/ota/start");

        String fileName = "test.bin";

        InputStream binFile = OTATest.class.getResourceAsStream("/static/ota/" + fileName);
        ContentBody fileBody = new InputStreamBody(binFile, ContentType.APPLICATION_OCTET_STREAM, fileName);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("upfile", fileBody);
        HttpEntity entity = builder.build();

        post.setEntity(entity);

        try (CloseableHttpResponse response = httpclient.execute(post)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            String error = consumeText(response);

            assertNotNull(error);
            assertEquals("No device in session.", error);
        }
    }

    @Test
    public void testImprovedUploadMethodAndCheckOTAStatusForDeviceThatNeverWasOnline() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPost post = new HttpPost(httpServerUrl + token + "/ota/start");

        String fileName = "test.bin";

        InputStream binFile = OTATest.class.getResourceAsStream("/static/ota/" + fileName);
        ContentBody fileBody = new InputStreamBody(binFile, ContentType.APPLICATION_OCTET_STREAM, fileName);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("upfile", fileBody);
        HttpEntity entity = builder.build();

        post.setEntity(entity);

        String path;
        try (CloseableHttpResponse response = httpclient.execute(post)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            path = consumeText(response);

            assertNotNull(path);
            assertTrue(path.startsWith("/static"));
            assertTrue(path.endsWith("bin"));
        }

        String responseUrl = "http://127.0.0.1" + path;
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new BlynkInternalMessage(7777, b("ota " + responseUrl))));

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody(2);

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(1, devices.length);
        Device device = devices[0];
        assertEquals("dima@mail.ua", device.otaInfo.OTAInitiatedBy);
        assertEquals(System.currentTimeMillis(), device.otaInfo.OTAInitiatedAt, 5000);
        assertEquals(0, device.otaInfo.OTAUpdateAt);
        assertFalse(device.otaInfo.isLastOtaUpdateOk());

        clientPair.hardwareClient.send("internal " + b("ver 0.3.1 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100 build 111"));

        device = devices[0];
        assertEquals("dima@mail.ua", device.otaInfo.OTAInitiatedBy);
        assertEquals(System.currentTimeMillis(), device.otaInfo.OTAInitiatedAt, 5000);
        assertEquals(0, device.otaInfo.OTAUpdateAt);
        assertFalse(device.otaInfo.isLastOtaUpdateOk());
    }

    @Test
    public void testImprovedUploadMethodAndCheckOTAStatusForDeviceThatWasOnline() throws Exception {
        clientPair.hardwareClient.send("internal " + b("ver 0.3.1 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100 build 111"));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPost post = new HttpPost(httpServerUrl + token + "/ota/start");

        String fileName = "test.bin";

        InputStream binFile = OTATest.class.getResourceAsStream("/static/ota/" + fileName);
        ContentBody fileBody = new InputStreamBody(binFile, ContentType.APPLICATION_OCTET_STREAM, fileName);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("upfile", fileBody);
        HttpEntity entity = builder.build();

        post.setEntity(entity);

        String path;
        try (CloseableHttpResponse response = httpclient.execute(post)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            path = consumeText(response);

            assertNotNull(path);
            assertTrue(path.startsWith("/static"));
            assertTrue(path.endsWith("bin"));
        }

        String responseUrl = "http://127.0.0.1" + path;
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(new BlynkInternalMessage(7777, b("ota " + responseUrl))));

        clientPair.appClient.send("getDevices 1");
        String response = clientPair.appClient.getBody(2);

        Device[] devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(1, devices.length);

        Device device = devices[0];
        assertEquals("0.3.1", device.hardwareInfo.version);
        assertEquals(10, device.hardwareInfo.heartbeatInterval);
        assertEquals("111", device.hardwareInfo.build);
        assertEquals("dima@mail.ua", device.otaInfo.OTAInitiatedBy);
        assertEquals(System.currentTimeMillis(), device.otaInfo.OTAInitiatedAt, 5000);
        assertEquals(0, device.otaInfo.OTAUpdateAt);
        assertFalse(device.otaInfo.isLastOtaUpdateOk());

        clientPair.hardwareClient.send("internal " + b("ver 0.3.1 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100 build 112"));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));

        clientPair.appClient.send("getDevices 1");
        response = clientPair.appClient.getBody(3);

        devices = JsonParser.mapper.readValue(response, Device[].class);
        assertNotNull(devices);
        assertEquals(1, devices.length);

        device = devices[0];
        assertEquals("0.3.1", device.hardwareInfo.version);
        assertEquals(10, device.hardwareInfo.heartbeatInterval);
        assertEquals("112", device.hardwareInfo.build);
        assertEquals("dima@mail.ua", device.otaInfo.OTAInitiatedBy);
        assertEquals(System.currentTimeMillis(), device.otaInfo.OTAInitiatedAt, 5000);
        assertEquals(System.currentTimeMillis(), device.otaInfo.OTAUpdateAt, 5000);
        assertTrue(device.otaInfo.isLastOtaUpdateOk());
    }
}
