package cc.blynk.integration.https;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.TokenGeneratorUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

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
        httpServerUrl = String.format("http://localhost:%s/", properties.getHttpPort());

        // Allow TLSv1 protocol only
        this.httpclient = HttpClients.createDefault();

        httpServer = new HardwareAndHttpAPIServer(holder).start();
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
        nvps.add(new BasicNameValuePair("appName", AppNameUtil.BLYNK));
        resetPassRequest.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = httpclient.execute(resetPassRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String data = TestUtil.consumeText(response);
            assertNotNull(data);
            assertEquals("Email was sent.", data);
        }

        String productName = properties.productName;
        verify(holder.mailWrapper).sendHtml(eq(email),
                eq("Password reset request for the " + productName + " app."),
                contains("/landing?token="));

        verify(holder.mailWrapper).sendHtml(eq(email),
                eq("Password reset request for the " + productName + " app."),
                contains("You recently made a request to reset your password for the " + productName + " app. To complete the process, click the link below."));

        verify(holder.mailWrapper).sendHtml(eq(email),
                eq("Password reset request for the " + productName + " app."),
                contains("If you did not request a password reset from " + productName + ", please ignore this message."));
    }

    @Test
    public void correctToken() throws Exception {
        String token = TokenGeneratorUtil.generateNewToken() + TokenGeneratorUtil.generateNewToken();
        HttpGet getRestorePage = new HttpGet(httpServerUrl + "/restore?token=" + token);

        try (CloseableHttpResponse response = httpclient.execute(getRestorePage)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String data = TestUtil.consumeText(response);
            assertNotNull(data);
        }
    }

    @Test
    public void getRestorePageXss() throws Exception {
        HttpGet getRestorePage = new HttpGet(httpServerUrl + "/restore?token=123");

        try (CloseableHttpResponse response = httpclient.execute(getRestorePage)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            String data = TestUtil.consumeText(response);
            assertNotNull(data);
            assertEquals("Invalid request parameters.", data);
        }
    }

    @Test
    public void getRestorePageXss2() throws Exception {
        String token = TokenGeneratorUtil.generateNewToken() + TokenGeneratorUtil.generateNewToken();
        HttpGet getRestorePage = new HttpGet(httpServerUrl + "/restore?token=" + token + "&email=123");

        try (CloseableHttpResponse response = httpclient.execute(getRestorePage)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            String data = TestUtil.consumeText(response);
            assertNotNull(data);
            assertEquals("Invalid request parameters.", data);
        }
    }

    @Test
    public void getRestorePageXss3() throws Exception {
        String token = "a".repeat(63) + "/";
        HttpGet getRestorePage = new HttpGet(httpServerUrl + "/restore?token=" + token);

        try (CloseableHttpResponse response = httpclient.execute(getRestorePage)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            String data = TestUtil.consumeText(response);
            assertNotNull(data);
            assertEquals("Invalid request parameters.", data);
        }
    }

}
