package cc.blynk.integration.http;

import cc.blynk.integration.BaseTest;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.utils.properties.GCMProperties;
import cc.blynk.utils.properties.MailProperties;
import cc.blynk.utils.properties.SmsProperties;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.Future;

import static cc.blynk.server.core.protocol.enums.Command.HTTP_GET_PIN_DATA;
import static cc.blynk.server.core.protocol.enums.Command.HTTP_UPDATE_PIN_DATA;
import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAPIMetricsTest extends BaseTest {

    private static BaseServer httpServer;
    private static AsyncHttpClient httpclient;
    private static String httpsServerUrl;
    private static Holder localHolder;

    @AfterClass
    public static void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
        localHolder.close();
    }

    @BeforeClass
    public static void init() throws Exception {
        properties.setProperty("data.folder", getRelativeDataFolder("/profiles"));
        localHolder = new Holder(properties,
                new MailProperties(Collections.emptyMap()),
                new SmsProperties(Collections.emptyMap()),
                new GCMProperties(Collections.emptyMap()),
                false
        );
        httpServer = new HttpAPIServer(localHolder).start();
        httpsServerUrl = String.format("http://localhost:%s/", httpPort);
        httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );
    }

    @Test
    public void testHttpAPICounters() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/update/v11?value=11").execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        assertEquals(1, localHolder.stats.specificCounters[HTTP_UPDATE_PIN_DATA].intValue());
        assertEquals(0, localHolder.stats.specificCounters[HTTP_GET_PIN_DATA].intValue());
        assertEquals(1, localHolder.stats.totalMessages.getCount());

        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/get/v11").execute();
        response = f.get();
        assertEquals(200, response.getStatusCode());

        assertEquals(1, localHolder.stats.specificCounters[HTTP_UPDATE_PIN_DATA].intValue());
        assertEquals(1, localHolder.stats.specificCounters[HTTP_GET_PIN_DATA].intValue());
        assertEquals(2, localHolder.stats.totalMessages.getCount());
    }

}
