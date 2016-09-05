package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.webhook.Header;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.Future;

import static cc.blynk.server.core.model.widgets.others.webhook.SupportedWebhookMethod.GET;
import static cc.blynk.server.core.model.widgets.others.webhook.SupportedWebhookMethod.PUT;
import static org.junit.Assert.assertEquals;
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
public class WebhookTest extends IntegrationBase {

    private BaseServer httpServer;
    private BaseServer appServer;
    private BaseServer hardwareServer;
    private AsyncHttpClient httpclient;
    private String httpServerUrl;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        httpServer = new HttpAPIServer(holder).start(transportTypeHolder);
        hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
        appServer = new AppServer(holder).start(transportTypeHolder);
        httpServerUrl = String.format("http://localhost:%s/", httpPort);

        httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent("")
                        .setKeepAlive(true)
                        .build());

        if (clientPair == null) {
            clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
        }
        clientPair.hardwareClient.reset();
        clientPair.appClient.reset();
    }

    @After
    public void shutdown() {
        httpServer.close();
        appServer.close();
        hardwareServer.close();
        clientPair.stop();
    }

    @Test
    @Ignore
    public void testThingsSpeakIntegrationTest() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = "https://api.thingspeak.com/update?api_key=API_KEY&field1=%s".replace("API_KEY", "");
        webHook.method = GET;
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiNoPlaceHolder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[\"124\"]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("124", values.get(0));
    }

    @Test
    public void testWebhookWorksWithBlynkHttpApiWithPlaceholder() throws Exception {
        WebHook webHook = new WebHook();
        webHook.url = httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124";
        webHook.method = PUT;
        webHook.headers = new Header[] {new Header("Content-Type", "application/json")};
        webHook.body = "[%s]";
        webHook.pin = 123;
        webHook.pinType = PinType.VIRTUAL;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.mapper.writeValueAsString(webHook));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.hardwareClient.send("hardware vw 123 10");
        verify(clientPair.hardwareClient.responseMock, after(1000).times(0)).channelRead(any(), any());

        Future<Response> f = httpclient.prepareGet(httpServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/V124").execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("10", values.get(0));
    }

    @Override
    public String getDataFolder() {
        return IntegrationBase.getProfileFolder();
    }


}
