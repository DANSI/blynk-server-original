package cc.blynk.integration.http;


import cc.blynk.integration.BaseTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore("due to security reasons, upload via http is not supported")
public class UploadAPITest extends BaseTest {

    private BaseServer httpServer;
    protected CloseableHttpClient httpclient;

    @Before
    public void init() throws Exception {
        httpServer = new HardwareAndHttpAPIServer(holder).start();
        httpclient = HttpClients.createDefault();
    }

    @After
    public void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
    }

    @Test
    public void uploadFileToServer() throws Exception {
        String pathToImage = upload("static/ota/test.bin");

        HttpGet index = new HttpGet("http://localhost:" + properties.getHttpPort() + pathToImage);

        try (CloseableHttpResponse response = httpclient.execute(index)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("application/octet-stream", response.getHeaders("Content-Type")[0].getValue());
        }
    }

    private String upload(String filename) throws Exception {
        InputStream logoStream = UploadAPITest.class.getResourceAsStream("/" + filename);

        HttpPost post = new HttpPost("http://localhost:" + properties.getHttpPort() + "/upload");
        ContentBody fileBody = new InputStreamBody(logoStream, ContentType.APPLICATION_OCTET_STREAM, filename);
        StringBody stringBody1 = new StringBody("Message 1", ContentType.MULTIPART_FORM_DATA);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("upfile", fileBody);
        builder.addPart("text1", stringBody1);
        HttpEntity entity = builder.build();

        post.setEntity(entity);

        String staticPath;
        try (CloseableHttpResponse response = httpclient.execute(post)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            staticPath = TestUtil.consumeText(response);

            assertNotNull(staticPath);
            assertTrue(staticPath.startsWith("/static"));
            assertTrue(staticPath.endsWith("bin"));
        }

        return staticPath;
    }

}
