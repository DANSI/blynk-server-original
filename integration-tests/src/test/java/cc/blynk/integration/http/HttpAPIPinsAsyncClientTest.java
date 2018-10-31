package cc.blynk.integration.http;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.properties.ServerProperties;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import static cc.blynk.integration.BaseTest.getRelativeDataFolder;
import static cc.blynk.integration.TestUtil.createHolderWithIOMock;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.setProperty;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAPIPinsAsyncClientTest extends SingleServerInstancePerTest {

    private static AsyncHttpClient httpclient;
    private static String httpsServerUrl;

    @AfterClass
    public static void closeHttp() throws Exception {
        httpclient.close();
    }

    @BeforeClass
    public static void init() throws Exception {
        properties = new ServerProperties(Collections.emptyMap());
        properties.setProperty("data.folder", getRelativeDataFolder("/profiles"));
        holder = createHolderWithIOMock(properties, "no-db.properties");
        hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        appServer = new MobileAndHttpsServer(holder).start();
        httpsServerUrl = String.format("http://localhost:%s/", properties.getHttpPort());
        httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );
    }

    //----------------------------GET METHODS SECTION

    @Test
    public void testGetWithFakeToken() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "dsadasddasdasdasdasdasdas/get/d8").execute();
        Response response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("Invalid token.", response.getResponseBody());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testGetWithWrongPathToken() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/w/d8").execute();
        assertEquals(404, f.get().getStatusCode());
    }

    @Test
    public void testGetWithWrongPin() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/x8").execute();
        Response response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("Wrong pin format.", response.getResponseBody());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testGetWithNonExistingPin() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/v11").execute();
        Response response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("Requested pin doesn't exist in the app.", response.getResponseBody());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testPutViaGetRequestSingleValue() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/v11?value=10").execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));


        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/v11").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = TestUtil.consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testPutAndGetTerminalValue() throws Exception {
        Future<Response> f= httpclient.prepareGet(httpsServerUrl + "7b0a3a61322e41a5b50589cf52d775d1/get/v17").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = TestUtil.consumeJsonPinValues(response.getResponseBody());
        assertEquals(0, values.size());

        f = httpclient.prepareGet(httpsServerUrl
                + "7b0a3a61322e41a5b50589cf52d775d1/update/v17?value=10").execute();
        response = f.get();
        assertEquals(200, response.getStatusCode());

        f = httpclient.prepareGet(httpsServerUrl
                + "7b0a3a61322e41a5b50589cf52d775d1/update/v17?value=11").execute();
        response = f.get();
        assertEquals(200, response.getStatusCode());

        f = httpclient.prepareGet(httpsServerUrl + "7b0a3a61322e41a5b50589cf52d775d1/get/v17").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = TestUtil.consumeJsonPinValues(response.getResponseBody());
        assertEquals(2, values.size());
        assertEquals("10", values.get(0));
        assertEquals("11", values.get(1));
    }

    @Test
    public void testPutViaGetRequestMultipleValue() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/v11?value=10&value=11").execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));


        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/v11").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = TestUtil.consumeJsonPinValues(response.getResponseBody());
        assertEquals(2, values.size());
        assertEquals("10", values.get(0));
        assertEquals("11", values.get(1));
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testPutGetNonExistingPin() throws Exception {
        Future<Response> f = httpclient.preparePut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/v10")
                .setHeader("Content-Type", "application/json")
                .setBody("[\"100\"]")
                .execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));

        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/v10").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = TestUtil.consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("100", values.get(0));
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testMultiPutGetNonExistingPin() throws Exception {
        Future<Response> f = httpclient.preparePut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/v10")
                .setHeader("Content-Type", "application/json")
                .setBody("[\"100\", \"101\", \"102\"]")
                .execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));

        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/v10").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = TestUtil.consumeJsonPinValues(response.getResponseBody());
        assertEquals(3, values.size());
        assertEquals("100", values.get(0));
        assertEquals("101", values.get(1));
        assertEquals("102", values.get(2));
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testGetPinData() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/v111?value=10").execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));

        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/data/v111").execute();
        response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("No data.", response.getResponseBody());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));

        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/data/z111").execute();
        response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("Wrong pin format.", response.getResponseBody());
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testGetCSVDataRedirect() throws Exception {
        Path reportingPath = Paths.get(holder.reportingDiskDao.dataFolder, "dmitriy@blynk.cc");
        Files.createDirectories(reportingPath);
        FileUtils.write(Paths.get(reportingPath.toString(), "history_125564119-0_v10_minute.bin"), 1, 2);

        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/data/v10").execute();
        Response response = f.get();
        assertEquals(301, response.getStatusCode());
        String redirectLocation = response.getHeader(LOCATION);
        assertNotNull(redirectLocation);
        assertTrue(redirectLocation.contains("/dmitriy@blynk.cc_125564119_0_v10_"));
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("0", response.getHeader(CONTENT_LENGTH));

        f = httpclient.prepareGet(httpsServerUrl + redirectLocation.replaceFirst("/", "")).execute();
        response = f.get();
        assertEquals(200, response.getStatusCode());
        assertEquals("application/x-gzip", response.getHeader(CONTENT_TYPE));
        assertEquals("*", response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void testChangeLabelPropertyViaGet() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + clientPair.token + "/update/v4?label=My-New-Label").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(setProperty(111, "1-0 4 label My-New-Label")));

        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (short) 4, PinType.VIRTUAL);
        assertNotNull(widget);
        assertEquals("My-New-Label", widget.label);
    }

    @Test
    public void testChangeColorPropertyViaGet() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + clientPair.token + "/update/v4?color=%23000000").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(setProperty(111, "1-0 4 color #000000")));

        clientPair.appClient.reset();

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

        Widget widget = profile.dashBoards[0].findWidgetByPin(0, (short) 4, PinType.VIRTUAL);
        assertNotNull(widget);
        assertEquals(255, widget.color);
    }

    @Test
    public void testChangeOnLabelPropertyViaGet() throws Exception {
        clientPair.appClient.reset();
        clientPair.appClient.updateWidget(1, "{\"id\":1, \"width\":1, \"height\":1,  \"x\":1, \"y\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\",         \"pinType\":\"VIRTUAL\", \"pin\":2, \"value\":\"1\"}");
        clientPair.appClient.verifyResult(ok(1));

        Future<Response> f = httpclient.prepareGet(httpsServerUrl + clientPair.token + "/update/v2?onLabel=newOnButtonLabel").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(setProperty(111, "1-0 2 onLabel newOnButtonLabel")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

        Button button = (Button) profile.dashBoards[0].findWidgetByPin(0, (short) 2, PinType.VIRTUAL);
        assertNotNull(button);
        assertEquals("newOnButtonLabel", button.onLabel);
    }


    @Test
    public void testChangeOffLabelPropertyViaGet() throws Exception {
        clientPair.appClient.reset();
        clientPair.appClient.updateWidget(1, "{\"id\":1, \"width\":1, \"height\":1, \"x\":1, \"y\":1, \"label\":\"Some Text\", \"type\":\"BUTTON\",         \"pinType\":\"VIRTUAL\", \"pin\":1, \"value\":\"1\"}");
        clientPair.appClient.verifyResult(ok(1));

        Future<Response> f = httpclient.prepareGet(httpsServerUrl + clientPair.token + "/update/v1?offLabel=newOffButtonLabel").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(setProperty(111, "1-0 1 offLabel newOffButtonLabel")));

        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

        Button button = (Button) profile.dashBoards[0].findWidgetByPin(0, (short) 1, PinType.VIRTUAL);
        assertNotNull(button);
        assertEquals("newOffButtonLabel", button.offLabel);
    }
}
