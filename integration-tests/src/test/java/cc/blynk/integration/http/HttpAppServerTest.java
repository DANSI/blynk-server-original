package cc.blynk.integration.http;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.server.core.http.HttpServer;
import cc.blynk.server.handlers.http.helpers.pojo.EmailPojo;
import cc.blynk.server.handlers.http.helpers.pojo.PushMessagePojo;
import cc.blynk.server.utils.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
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
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAppServerTest extends IntegrationBase {

    private HttpServer httpServer;
    private CloseableHttpClient httpclient;
    private String httpsServerUrl;

    @Before
    public void init() throws Exception {
        properties.setProperty("data.folder", getProfileFolder());
        initServerStructures();

        this.httpServer = new HttpServer(holder);
        httpServer.start();
        sleep(500);

        httpsServerUrl = "http://localhost:" + httpPort + "/";

        this.httpclient = HttpClients.createDefault();
    }

    @After
    public void shutdown() throws Exception {
        this.httpclient.close();
        this.httpServer.stop();
    }

    //----------------------------GET METHODS SECTION

    @Test
    public void testGetWithFakeToken() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "dsadasddasdasdasdasdasdas/pin/d8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetWithWrongPathToken() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/w/d8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetWithWrongPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/x8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetWithNonExistingPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/v10");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetWithExistingPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/d8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("0", values.get(0));
        }

        request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/d1");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("1", values.get(0));
        }

        request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/d3");
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("87", values.get(0));
        }
    }

    @Test
    public void testGetWithExistingEmptyPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/a14");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(0, values.size());
        }
    }

    @Test
    public void testGetWithExistingMultiPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/a15");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(2, values.size());
            assertEquals("1", values.get(0));
            assertEquals("2", values.get(1));
        }
    }




    //----------------------------PUT METHODS SECTION

    @Test
    public void testPutNoContentType() throws Exception {
        HttpPut request = new HttpPut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/d8");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(500, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPutFakeToken() throws Exception {
        HttpPut request = new HttpPut(httpsServerUrl + "dsadasddasdasdasdasdasdas/pin/d8");
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPutWithWrongPin() throws Exception {
        HttpPut request = new HttpPut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/x8");
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPutWithNonExistingPin() throws Exception {
        HttpPut request = new HttpPut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/v10");
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPutWithExistingPin() throws Exception {
        HttpPut request = new HttpPut(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/a14");
        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(204, response.getStatusLine().getStatusCode());
        }

        HttpGet getRequest = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/pin/a14");

        try (CloseableHttpResponse response = httpclient.execute(getRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("100", values.get(0));
        }
    }


    //----------------------------NOTIFICATION POST METHODS SECTION

    //----------------------------pushes
    @Test
    public void testPostNotifyNoContentType() throws Exception {
        HttpPost request = new HttpPost(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/notify");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(500, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPostNotifyNoBody() throws Exception {
        HttpPost request = new HttpPost(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/notify");
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPostNotifyWithWrongBody() throws Exception {
        HttpPost request = new HttpPost(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/notify");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append(i);
        }
        request.setEntity(new StringEntity("{\"body\":\"" + sb.toString() + "\"}", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPostNotifyWithBody() throws Exception {
        HttpPost request = new HttpPost(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/notify");
        String message = JsonParser.mapper.writeValueAsString(new PushMessagePojo("This is Push Message"));
        request.setEntity(new StringEntity(message, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }


    //----------------------------email
    @Test
    public void testPostEmailNoContentType() throws Exception {
        HttpPost request = new HttpPost(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/email");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(500, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPostEmailNoBody() throws Exception {
        HttpPost request = new HttpPost(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/email");
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testPostEmailWithBody() throws Exception {
        HttpPost request = new HttpPost(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/email");
        String message = JsonParser.mapper.writeValueAsString(new EmailPojo("pupkin@gmail.com", "Title", "Subject"));
        request.setEntity(new StringEntity(message, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> consumeJsonPinValues(CloseableHttpResponse response) throws IOException {
        return JsonParser.readAny(EntityUtils.toString(response.getEntity()), List.class);
    }


}
