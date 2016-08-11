package cc.blynk.integration.http;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.IntegrationBase;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.core.BaseServer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAPIPinsAsyncClientTest extends BaseTest {

    private static BaseServer httpServer;
    private static AsyncHttpClient httpclient;
    private static String httpsServerUrl;

    @AfterClass
    public static void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
    }

    @Before
    public void init() throws Exception {
        if (httpServer == null) {
            httpServer = new HttpAPIServer(holder).start(transportTypeHolder);
            httpsServerUrl = String.format("http://localhost:%s/", httpPort);
            httpclient = new DefaultAsyncHttpClient(
                    new DefaultAsyncHttpClientConfig.Builder()
                            .setUserAgent("")
                            .setKeepAlive(false)
                            .build()
            );
        }
    }

    @Override
    public String getDataFolder() {
        return IntegrationBase.getProfileFolder();
    }

    //----------------------------GET METHODS SECTION

    @Test
    public void testGetWithFakeToken() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "dsadasddasdasdasdasdasdas/pin/d8").execute();
        Response response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("Invalid token.", response.getResponseBody());
    }

    @Test
    public void testGetWithWrongPathToken() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/w/d8").execute();
        assertEquals(404, f.get().getStatusCode());
    }

    @Test
    public void testGetWithWrongPin() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/x8").execute();
        Response response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("Wrong pin format.", response.getResponseBody());
    }

    @Test
    public void testGetWithNonExistingPin() throws Exception {
        Future<Response> f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/v11").execute();
        Response response = f.get();
        assertEquals(400, response.getStatusCode());
        assertEquals("Requested pin not exists in app.", response.getResponseBody());
    }

    @Test
    public void testPutGetNonExistingPin() throws Exception {
        Future<Response> f = httpclient.preparePut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/v10")
                .setHeader("Content-Type", "application/json")
                .setBody("[\"100\"]")
                .execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());

        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/v10").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(1, values.size());
        assertEquals("100", values.get(0));

    }

    @Test
    public void testMultiPutGetNonExistingPin() throws Exception {
        Future<Response> f = httpclient.preparePut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/v10")
                .setHeader("Content-Type", "application/json")
                .setBody("[\"100\", \"101\", \"102\"]")
                .execute();
        Response response = f.get();

        assertEquals(200, response.getStatusCode());

        f = httpclient.prepareGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/v10").execute();
        response = f.get();

        assertEquals(200, response.getStatusCode());
        List<String> values = consumeJsonPinValues(response.getResponseBody());
        assertEquals(3, values.size());
        assertEquals("100", values.get(0));
        assertEquals("101", values.get(1));
        assertEquals("102", values.get(2));
    }

}
