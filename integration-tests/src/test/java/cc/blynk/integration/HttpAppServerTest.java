package cc.blynk.integration;

import cc.blynk.server.core.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAppServerTest extends IntegrationBase  {

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
    public void testAskNonExistingPin() throws Exception {
        HttpGet request = new HttpGet(httpsServerUrl + "4ae3851817194e2596cf1b7103603ef8/widget/v10");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        } finally {
            request.releaseConnection();
        }

    }


}
