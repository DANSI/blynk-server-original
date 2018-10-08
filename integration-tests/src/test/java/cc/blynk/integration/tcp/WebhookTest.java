package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.webhook.Header;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.properties.ServerProperties;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import static cc.blynk.integration.BaseTest.getRelativeDataFolder;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.consumeJsonPinValues;
import static cc.blynk.integration.TestUtil.createHolderWithIOMock;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.model.widgets.others.webhook.SupportedWebhookMethod.GET;
import static cc.blynk.server.core.model.widgets.others.webhook.SupportedWebhookMethod.PUT;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
public class WebhookTest extends SingleServerInstancePerTest {

    private static AsyncHttpClient httpclient;
    private static String httpServerUrl;

    @BeforeClass
    //shadow parent method by purpose
    public static void init() throws Exception {
        properties = new ServerProperties(Collections.emptyMap());
        properties.setProperty("data.folder", getRelativeDataFolder("/profiles"));

        holder = createHolderWithIOMock(properties, "no-db.properties");
        hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        appServer = new MobileAndHttpsServer(holder).start();

        httpServerUrl = String.format("http://localhost:%s/", properties.getHttpPort());
        httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent("")
                        .setKeepAlive(true)
                        .build());
    }

    @AfterClass
    public static void closeHttpClient() throws Exception {
        httpclient.close();
    }

    @Test
    @Ignore
    public void testThingsSpeakIntegrationTest() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "https://api.thingspeak.com/update?api_key=API_KEY&field1=%s".replace("API_KEY", "");
        webHook.method = GET;
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());
    }

    @Test
    @Ignore
    public void testSome3dPartyWeatherServiceTest() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "http://api.sunrise-sunset.org/json?lat=36.7201600&lng=-4.4203400&date=2016-08-25";
        webHook.method = GET;
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        String expectedResponse = "vw" + StringUtils.BODY_SEPARATOR_STRING + "123" + StringUtils.BODY_SEPARATOR_STRING +
                "{\"results\":{\"sunrise\":\"7:30:27 AM\",\"sunset\":\"5:14:34 PM\",\"solar_noon\":\"12:22:31 PM\",\"day_length\":\"09:44:07\",\"civil_twilight_begin\":\"7:01:53 AM\",\"civil_twilight_end\":\"5:43:08 PM\",\"nautical_twilight_begin\":\"6:29:39 AM\",\"nautical_twilight_end\":\"6:15:23 PM\",\"astronomical_twilight_begin\":\"5:58:15 AM\",\"astronomical_twilight_end\":\"6:46:46 PM\"},\"status\":\"OK\"}";
        clientPair.hardwareClient.verifyResult(hardware(888, expectedResponse));
    }

    @Test
    @Ignore
    public void testSome3dPartyWeatherServiceTriggerFromAppTest() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "http://api.sunrise-sunset.org/json?lat=36.7201600&lng=-4.4203400&date=2016-08-25";
        webHook.method = GET;
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw 123 10");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 123 10"))));

        String expectedResponse = "vw" + StringUtils.BODY_SEPARATOR_STRING + "123" + StringUtils.BODY_SEPARATOR_STRING +
                "{\"results\":{\"sunrise\":\"7:30:27 AM\",\"sunset\":\"5:14:34 PM\",\"solar_noon\":\"12:22:31 PM\",\"day_length\":\"09:44:07\",\"civil_twilight_begin\":\"7:01:53 AM\",\"civil_twilight_end\":\"5:43:08 PM\",\"nautical_twilight_begin\":\"6:29:39 AM\",\"nautical_twilight_end\":\"6:15:23 PM\",\"astronomical_twilight_begin\":\"5:58:15 AM\",\"astronomical_twilight_end\":\"6:46:46 PM\"},\"status\":\"OK\"}";
        clientPair.hardwareClient.verifyResult(hardware(888, expectedResponse));
    }

    @Test
    public void testReservedREgexCharForReplaceArgumentsInWebhook() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"/pin/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 $$");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("$$", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiNoPlaceHolder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"124\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("124", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiPlaceHolderAndTextPlain() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V125";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "text/plain")};
        webHook.body = "[\"/pin/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V125").execute();
        Response response = f.get();

        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithArrayPlaceholderInURL() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124?value=/pin[0]/&value=/pin[1]/&value=/pin[2]/";
        webHook.method = GET;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 " + b("10 11 12"));
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(3, values.size());
        assertEquals("10", values.get(0));
        assertEquals("11", values.get(1));
        assertEquals("12", values.get(2));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithArray10PlaceholdersInURL() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124?" +
                "value=/pin[0]/" +
                "&value=/pin[1]/" +
                "&value=/pin[2]/" +
                "&value=/pin[3]/" +
                "&value=/pin[4]/" +
                "&value=/pin[5]/" +
                "&value=/pin[6]/" +
                "&value=/pin[7]/" +
                "&value=/pin[8]/" +
                "&value=/pin[9]/";

        webHook.method = GET;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 " + b("0 1 2 3 4 5 6 7 8 9"));
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(10, values.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("" + i, values.get(i));
        }
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithDateTimePlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"/datetime_iso/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertTrue(values.get(0).endsWith("Z"));
        assertTrue(values.get(0).contains("T"));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithDateTimePlaceholderAndPins() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"/datetime_iso/,/pin[0]/,/pin[1]/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10 11");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        String[] resp = values.get(0).split(",");
        String dateTime = resp[0];
        String pin0 = resp[1];
        String pin1 = resp[2];
        assertTrue(dateTime.endsWith("Z"));
        assertTrue(dateTime.contains("T"));
        assertEquals("10", pin0);
        assertEquals("11", pin1);
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithPlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[%s]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithArrayPlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[/pin[0]/,/pin[1]/,/pin[2]/]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 " + b("10 11 12"));
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(3, values.size());
        assertEquals("10", values.get(0));
        assertEquals("11", values.get(1));
        assertEquals("12", values.get(2));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithArrayPlaceholder2() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[/pin[0]/]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithPlaceholderQuotaLimit() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[%s]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(500).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));

        clientPair.hardwareClient.send("hardware vw 123 11");
        verify(clientPair.hardwareClient.responseMock, after(600).times(0)).channelRead(any(), any());

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));


        clientPair.hardwareClient.send("hardware vw 123 12");
        verify(clientPair.hardwareClient.responseMock, after(500).times(0)).channelRead(any(), any());

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("12", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiNoPlaceHolderAppSideTrigger() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"124\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        //125564119 is id of project with 4ae3851817194e2596cf1b7103603ef8 token
        clientPair.appClient.send("hardware 1 vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(500).times(1)).channelRead(any(), eq(hardware(2, "vw 123 10")));

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("124", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiAppSideTriggerCheckLimit() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"/pin/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(500)).channelRead(any(), eq(hardware(2, "vw 123 10")));

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));

        clientPair.appClient.send("hardware 1 vw 123 11");
        verify(clientPair.hardwareClient.responseMock, after(1000)).channelRead(any(), eq(hardware(3, "vw 123 11")));

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));


        clientPair.appClient.send("hardware 1 vw 123 11");
        verify(clientPair.hardwareClient.responseMock, after(500)).channelRead(any(), eq(hardware(4, "vw 123 11")));

        f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("11", values.get(0));
    }

    @Test
    public void testWebhookInvalidUrl() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "";
        webHook.method = PUT;
        webHook.headers = new Header[]{new Header("Content-Type", "application/json")};
        webHook.body = "[\"/pin/\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;
        webHook.id = 111;


        webHook.url = "http://adasd.com";
        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        webHook.id = 222;
        webHook.url = "https://adasd.com";
        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(2));

        webHook.id = 333;
        webHook.url = "Http://adasd.com";
        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(3));
    }

    @Test
    public void testWebhookWorksWithUrlPlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "/pin/";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"text\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw 123 " + httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124");
        verify(clientPair.hardwareClient.responseMock, after(500).times(1)).channelRead(any(), eq(
                new HardwareMessage(2, b("vw 123 " + httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/V124"))));

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("text", values.get(0));
    }

    @Test
    public void testWebhookWorksWithUrlPlaceholder2() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "/pin/";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;
        webHook.width = 2;
        webHook.height = 1;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1 vw 123 1");
        clientPair.hardwareClient.verifyResult(hardware(2, "vw 123 1"));

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/V126").execute();
        Response response = f.get();

        assertEquals(400, response.getStatusCode());
        assertEquals("Requested pin doesn't exist in the app.", response.getResponseBody());
    }

}
