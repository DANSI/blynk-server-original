package cc.blynk.server.reset.web.controller;

import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.reset.web.entities.ResponseUserEntity;
import cc.blynk.server.reset.web.entities.TokenUser;
import cc.blynk.server.reset.web.entities.TokensPool;
import cc.blynk.utils.Config;
import cc.blynk.utils.ServerProperties;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * The Blynk project
 * Created by Andrew Zakordonets
 * Date : 12/15/2015.
 */
public class ResetPasswordController {

    private static final Logger log = LogManager.getLogger(ResetPasswordController.class);
    private final MailWrapper mailWrapper;
    private final String url;
    private final int port;
    private final TokensPool tokensPool;
    private final String body;
    private final String pageContent;
    private final CloseableHttpClient httpclient;
    private final String serverUrl;

    public ResetPasswordController(String url, int port, TokensPool tokensPool) throws Exception {
        this(url, port, tokensPool, new ServerProperties(Config.MAIL_PROPERTIES_FILENAME));
    }

    public ResetPasswordController(String url, int port, TokensPool tokensPool, ServerProperties serverProperties) throws Exception {
        this.mailWrapper = new MailWrapper(serverProperties);
        this.url = url;
        this.port = port;

        URL bodyUrl = Resources.getResource("body/message.html");
        this.body = Resources.toString(bodyUrl, Charsets.UTF_8);
        this.tokensPool = tokensPool;
        URL pageUrl = Resources.getResource("html/enterNewPassword.html");
        this.pageContent = Resources.toString(pageUrl, Charsets.UTF_8);

        SSLContext sslcontext = initUnsecuredSSLContext();
        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new MyHostVerifier());
        this.httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        this.serverUrl = serverProperties.getProperty("server.url");
    }

    public void sendResetPasswordEmail(String email, String token) throws Exception {
        TokenUser user = new TokenUser(email);
        tokensPool.addToken(token, user);
        String resetUrl = String.format("%s%s/landing?token=%s", url, (port == 80) ? "" : ":" + port, token);
        String message = body.replace("{RESET_URL}", resetUrl);
        log.info("Sending token to {} address", email);
        mailWrapper.send(email, "Password reset request for Blynk app.", message, "text/html");
    }

    public void invoke(String email, String password) throws IOException {
        HttpPut request = new HttpPut(serverUrl + email.toLowerCase());
        request.setEntity(new StringEntity(new ResponseUserEntity(password).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            EntityUtils.consume(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                //do nothing all is ok;
            } else if (response.getStatusLine().getStatusCode() == 404) {
                throw new IOException("User not exists.");
            } else {
                throw new IOException("Unexpected error.");
            }
        }
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

    public String getResetPasswordPage(String email, String token) {
        return pageContent.replace("{EMAIL}", email).replace("{TOKEN}", token);
    }

    private class MyHostVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }
}
