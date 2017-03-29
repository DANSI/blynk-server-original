package cc.blynk.integration.https;

import cc.blynk.integration.BaseTest;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.AppName;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpResetPassTest extends BaseTest {

    private static BaseServer httpServer;
    private CloseableHttpClient httpclient;
    private String httpServerUrl;

    @After
    public void shutdown() throws Exception {
        httpServer.close();
        httpclient.close();
    }

    @Before
    public void init() throws Exception {
        httpServerUrl = String.format("http://localhost:%s/", httpPort);

        // Allow TLSv1 protocol only
        this.httpclient = HttpClients.createDefault();

        httpServer = new HttpAPIServer(holder).start();
    }

    @Override
    public String getDataFolder() {
        return getRelativeDataFolder("/profiles");
    }

    @Test
    public void submitResetPasswordRequest() throws Exception {
        String email = "dmitriy@blynk.cc";
        HttpPost resetPassRequest = new HttpPost(httpServerUrl + "/resetPassword");
        List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("email", email));
        nvps.add(new BasicNameValuePair("appName", AppName.BLYNK));
        resetPassRequest.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = httpclient.execute(resetPassRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String data = consumeText(response);
            assertNotNull(data);
            assertEquals("Email was sent.", data);
        }

        verify(mailWrapper).sendHtml(eq(email), eq("Password reset request for Blynk app."), contains("/landing?token="));
    }


}
