package cc.blynk.integration.http;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.server.core.HttpServer;
import cc.blynk.server.utils.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAppServerTest extends IntegrationBase {

    private HttpServer httpServer;
    private CloseableHttpClient httpclient;
    private String httpsServerUrl;

    private static String getProfileFolder() throws Exception {
        URL resource = HttpAppServerTest.class.getResource("/profiles");
        String resourcesPath = Paths.get(resource.toURI()).toAbsolutePath().toString();
        System.out.println("Resource path : " + resourcesPath);
        return resourcesPath;
    }

    @Before
    public void init() throws Exception {
        properties.setProperty("data.folder", getProfileFolder());
        initServerStructures();

        this.httpServer = new HttpServer(holder);
        httpServer.run();
        sleep(500);

        httpsServerUrl = "http://localhost:" + httpPort + "/app/";

        this.httpclient = HttpClients.createDefault();
    }

    @After
    public void shutdown() throws Exception {
        this.httpclient.close();
        this.httpServer.stop();
    }

    @Test
    public void testFakeToken() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "dsadasddasdasdasdasdasdas/widget/d8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testWrongPathToken() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/w/d8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testWrongPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/x8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetNonExistingPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/v10");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetExistingPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/d8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("0", values.get(0));
        }

        request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/d1");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("1", values.get(0));
        }

        request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/d3");
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("87", values.get(0));
        }
    }

    @Test
    public void testGetExistingEmptyPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/a14");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(0, values.size());
        }
    }

    @Test
    public void testGetExistingMultiPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/a15");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(2, values.size());
            assertEquals("1", values.get(0));
            assertEquals("2", values.get(1));
        }
    }






    @SuppressWarnings("unchecked")
    private List<String> consumeJsonPinValues(CloseableHttpResponse response) throws IOException {
        return JsonParser.readAny(EntityUtils.toString(response.getEntity()), List.class);
    }


}
