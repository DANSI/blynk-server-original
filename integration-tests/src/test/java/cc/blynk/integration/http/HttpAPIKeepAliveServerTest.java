package cc.blynk.integration.http;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.utils.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAPIKeepAliveServerTest extends IntegrationBase {

    private static HttpAPIServer httpServer;
    private static CloseableHttpClient httpclient;
    private static String httpsServerUrl;

    @AfterClass
    public static void shutdown() throws Exception {
        httpclient.close();
        httpServer.stop();
    }

    @Before
    public void init() throws Exception {
        if (httpServer == null) {
            properties.setProperty("data.folder", getProfileFolder());
            initServerStructures();

            httpServer = new HttpAPIServer(holder);
            httpServer.start();
            sleep(500);

            int httpPort = holder.props.getIntProperty("http.port");

            httpsServerUrl = "http://localhost:" + httpPort + "/";

            //this http client doesn't close HTTP connection.
            httpclient = HttpClients.custom()
                    .setConnectionReuseStrategy((response, context) -> true)
                    .setKeepAliveStrategy((response, context) -> 10000000).build();
        }
    }

    @Test
    public void testKeepAlive() throws Exception {
        String url = httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/a14";

        HttpPut request = new HttpPut(url);
        request.setHeader("Connection", "keep-alive");

        HttpGet getRequest = new HttpGet(url);
        getRequest.setHeader("Connection", "keep-alive");

        for (int i = 0; i < 100; i++) {
            request.setEntity(new StringEntity("[\""+ i + "\"]", ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpclient.execute(request)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            }

            try (CloseableHttpResponse response2 = httpclient.execute(getRequest)) {
                assertEquals(200, response2.getStatusLine().getStatusCode());
                List<String> values = consumeJsonPinValues(response2);
                assertEquals(1, values.size());
                assertEquals(String.valueOf(i), values.get(0));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> consumeJsonPinValues(CloseableHttpResponse response) throws IOException {
        return JsonParser.readAny(consumeText(response), List.class);
    }

    @SuppressWarnings("unchecked")
    private String consumeText(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

}
