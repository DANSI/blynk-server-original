package cc.blynk.integration.https;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.http.ResponseUserEntity;
import cc.blynk.server.admin.http.HttpsAdminServer;
import cc.blynk.server.core.BaseServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
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

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpsAdminServerTest extends IntegrationBase {

    private BaseServer httpAdminServer;
    private CloseableHttpClient httpclient;
    private String httpsServerUrl;

    @Before
    public void init() throws Exception {
        properties.setProperty("data.folder", getProfileFolder());
        initServerStructures();
        this.httpAdminServer = new HttpsAdminServer(holder).start();
        sleep(500);

        httpsServerUrl = "https://localhost:" + properties.getIntProperty("administration.https.port") + "/admin/users/";

        SSLContext sslcontext = initUnsecuredSSLContext();

        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new MyHostVerifier());
        this.httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }

    private SSLContext initUnsecuredSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {

            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{ tm }, null);

        return context;
    }

    @After
    public void shutdown() {
        this.httpAdminServer.stop();
    }

    @Test
    public void testChangePassNoUser() throws Exception {
        String testUser = "dima@dima.ua";
        HttpPut request = new HttpPut(httpsServerUrl + "changePass/" + testUser);
        request.setEntity(new StringEntity(new ResponseUserEntity("123").toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode() );
        }
    }

    @Test
    public void testServerResetPass() throws Exception {
        for (int i = 0; i < 100; i++) {
            String name = "dima@dima.ua";
            HttpPut request = new HttpPut(httpsServerUrl + "changePass/" + name);
            request.setEntity(new StringEntity(new ResponseUserEntity("pass").toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpclient.execute(request)) {
                EntityUtils.consume(response.getEntity());
                assertEquals(404, response.getStatusLine().getStatusCode());
            }
        }

        for (int i = 0; i < 100; i++) {
            String name = "dmitriy@blynk.cc";
            HttpPut request = new HttpPut(httpsServerUrl + "changePass/" + name);
            request.setEntity(new StringEntity(new ResponseUserEntity("pass").toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpclient.execute(request)) {
                EntityUtils.consume(response.getEntity());
                assertEquals(200, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testChangePassCorrect() throws Exception {
        String testUser = "dmitriy@blynk.cc";
        HttpPut request = new HttpPut(httpsServerUrl + "changePass/" + testUser);
        request.setEntity(new StringEntity(new ResponseUserEntity("123").toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
        }
    }

    private class MyHostVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

}
