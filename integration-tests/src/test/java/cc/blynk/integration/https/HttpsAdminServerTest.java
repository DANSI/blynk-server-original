package cc.blynk.integration.https;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.http.ResponseUserEntity;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.api.http.HttpsAPIServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.SHA256Util;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpsAdminServerTest extends BaseTest {

    private static BaseServer httpServer;
    private BaseServer httpAdminServer;
    private CloseableHttpClient httpclient;
    private String httpsAdminServerUrl;
    private String httpServerUrl;
    private User admin;

    @After
    public void shutdown() {
        httpAdminServer.close();
        httpServer.close();
    }

    @Before
    public void init() throws Exception {
        this.httpAdminServer = new HttpsAPIServer(holder).start();

        httpsAdminServerUrl = String.format("https://localhost:%s/admin", httpsPort);
        httpServerUrl = String.format("http://localhost:%s/", httpPort);

        SSLContext sslcontext = initUnsecuredSSLContext();

        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new MyHostVerifier());
        this.httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        httpServer = new HttpAPIServer(holder).start();

        String name = "admin@blynk.cc";
        String pass = "admin";
        admin = new User(name, SHA256Util.makeHash(pass, name), AppNameUtil.BLYNK, "local", "127.0.0.1", false, true);
        holder.userDao.add(admin);
    }

    @Override
    public String getDataFolder() {
        return getRelativeDataFolder("/profiles");
    }

    @Test
    public void testGetnOnExistingUser() throws Exception {
        String testUser = "dima@dima.ua";
        HttpPut request = new HttpPut(httpsAdminServerUrl + "/users/" + "xxx/" + testUser);
        request.setEntity(new StringEntity(new ResponseUserEntity("123").toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetWrongUrl() throws Exception {
        String testUser = "dima@dima.ua";
        HttpPut request = new HttpPut(httpsAdminServerUrl + "/urs213213/" + "xxx/" + testUser);
        request.setEntity(new StringEntity(new ResponseUserEntity("123").toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void adminLoginFlowSupport()  throws Exception {
        HttpGet loadLoginPageRequest = new HttpGet(httpsAdminServerUrl);
        try (CloseableHttpResponse response = httpclient.execute(loadLoginPageRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String loginPage = consumeText(response);
            //todo add full page match?
            assertTrue(loginPage.contains("Use your Admin account to log in"));
        }

        login(admin.email, admin.pass);

        HttpGet loadAdminPage = new HttpGet(httpsAdminServerUrl);
        try (CloseableHttpResponse response = httpclient.execute(loadAdminPage)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String adminPage = consumeText(response);
            //todo add full page match?
            assertTrue(adminPage.contains("Blynk Administration"));
            assertTrue(adminPage.contains("admin.js"));
        }
    }

    @Test
    public void adminLoginOnlyForSuperUser()  throws Exception {
        String name = "admin@blynk.cc";
        String pass = "admin";

        User admin = new User(name, SHA256Util.makeHash(pass, name), AppNameUtil.BLYNK, "local", "127.0.0.1", false, false);
        holder.userDao.add(admin);

        HttpPost loginRequest = new HttpPost(httpsAdminServerUrl + "/login");
        List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("email", admin.email));
        nvps.add(new BasicNameValuePair("password", admin.pass));
        loginRequest.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = httpclient.execute(loginRequest)) {
            assertEquals(301, response.getStatusLine().getStatusCode());
            Header header = response.getFirstHeader("Location");
            assertNotNull(header);
            assertEquals("/admin", header.getValue());
            Header cookieHeader = response.getFirstHeader("set-cookie");
            assertNull(cookieHeader);
        }
    }

    @Test
    public void testGetUserFromAdminPageNoAccess() throws Exception {
        String testUser = "dmitriy@blynk.cc";
        String appName = "Blynk";
        HttpGet request = new HttpGet(httpsAdminServerUrl + "/users/" + testUser + "-" + appName);

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetUserFromAdminPageNoAccessWithFakeCookie() throws Exception {
        String testUser = "dmitriy@blynk.cc";
        String appName = "Blynk";
        HttpGet request = new HttpGet(httpsAdminServerUrl + "/users/" + testUser + "-" + appName);
        request.setHeader("set-cookie", "session=123");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetUserFromAdminPage() throws Exception {
        login(admin.email, admin.pass);
        String testUser = "dmitriy@blynk.cc";
        String appName = "Blynk";
        HttpGet request = new HttpGet(httpsAdminServerUrl + "/users/" + testUser + "-" + appName);

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String jsonProfile = consumeText(response);
            assertNotNull(jsonProfile);
            User user = JsonParser.readAny(jsonProfile, User.class);
            assertNotNull(user);
            assertEquals(testUser, user.email);
            assertNotNull(user.profile.dashBoards);
            assertEquals(5, user.profile.dashBoards.length);
        }
    }

    private void login(String name, String pass) throws Exception {
        HttpPost loginRequest = new HttpPost(httpsAdminServerUrl + "/login");
        List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("email", name));
        nvps.add(new BasicNameValuePair("password", pass));
        loginRequest.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = httpclient.execute(loginRequest)) {
            assertEquals(301, response.getStatusLine().getStatusCode());
            Header header = response.getFirstHeader("Location");
            assertNotNull(header);
            assertEquals("/admin", header.getValue());
            Header cookieHeader = response.getFirstHeader("set-cookie");
            assertNotNull(cookieHeader);
            assertTrue(cookieHeader.getValue().startsWith("session="));
        }
    }

    @Test
    public void testChangeUsernameChangesPassToo() throws Exception {
        login(admin.email, admin.pass);

        User user;
        HttpGet getUserRequest = new HttpGet(httpsAdminServerUrl + "/users/admin@blynk.cc-Blynk");
        try (CloseableHttpResponse response = httpclient.execute(getUserRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String userProfile = consumeText(response);
            assertNotNull(userProfile);
            user = JsonParser.parseUserFromString(userProfile);
            assertEquals(admin.email, user.email);
        }

        user.email = "123@blynk.cc";

        //we are no allowed to change username without cahnged password
        HttpPut changeUserNameRequestWrong = new HttpPut(httpsAdminServerUrl + "/users/admin@blynk.cc-Blynk");
        changeUserNameRequestWrong.setEntity(new StringEntity(user.toString(), ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(changeUserNameRequestWrong)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }

        user.pass = "123";
        HttpPut changeUserNameRequestCorrect = new HttpPut(httpsAdminServerUrl + "/users/admin@blynk.cc-Blynk");
        changeUserNameRequestCorrect.setEntity(new StringEntity(user.toString(), ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(changeUserNameRequestCorrect)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet getNonExistingUserRequest = new HttpGet(httpsAdminServerUrl + "/users/admin@blynk.cc-Blynk");
        try (CloseableHttpResponse response = httpclient.execute(getNonExistingUserRequest)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }

        HttpGet getUserRequest2 = new HttpGet(httpsAdminServerUrl + "/users/123@blynk.cc-Blynk");
        try (CloseableHttpResponse response = httpclient.execute(getUserRequest2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String userProfile = consumeText(response);
            assertNotNull(userProfile);
            user = JsonParser.parseUserFromString(userProfile);
            assertEquals("123@blynk.cc", user.email);
            assertEquals(SHA256Util.makeHash("123", user.email), user.pass);
        }
    }

    @Test
    public void testGetAdminPage() throws Exception {
        HttpGet request = new HttpGet(httpsAdminServerUrl);

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetFavIconHttps() throws Exception {
        HttpGet request = new HttpGet(httpsAdminServerUrl.replace("/admin", "") + "/favicon.ico");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getStaticFile() throws Exception {
        HttpGet request = new HttpGet(httpsAdminServerUrl.replace("admin", "static/admin.html"));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetFavIconHttp() throws Exception {
        HttpGet request = new HttpGet(httpServerUrl + "favicon.ico");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testAssignNewTokenForNonExistingToken() throws Exception {
        login(admin.email, admin.pass);
        HttpGet request = new HttpGet(httpsAdminServerUrl + "/users/token/assign?old=123&new=123");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testAssignNewToken() throws Exception {
        login(admin.email, admin.pass);

        HttpGet request = new HttpGet(httpsAdminServerUrl + "/users/token/assign?old=4ae3851817194e2596cf1b7103603ef8&new=123");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpPut put = new HttpPut(httpServerUrl + "123/pin/v10");
        put.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(put)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet get = new HttpGet(httpServerUrl + "123/pin/v10");

        try (CloseableHttpResponse response = httpclient.execute(get)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("100", values.get(0));
        }

        request = new HttpGet(httpsAdminServerUrl + "/users/token/assign?old=4ae3851817194e2596cf1b7103603ef8&new=124");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testForceAssignNewToken() throws Exception {
        login(admin.email, admin.pass);
        HttpGet request = new HttpGet(httpsAdminServerUrl + "/users/token/force?email=dmitriy@blynk.cc&app=Blynk&dashId=79780619&deviceId=0&new=123");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpPut put = new HttpPut(httpServerUrl + "123/pin/v10");
        put.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(put)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet get = new HttpGet(httpServerUrl + "123/pin/v10");

        try (CloseableHttpResponse response = httpclient.execute(get)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("100", values.get(0));
        }
    }

}
