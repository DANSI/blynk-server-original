package cc.blynk.server.application.handlers.main.auth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.04.16.
 */
public class FacebookLoginCheck {

    private static final String URL = "https://graph.facebook.com/me?fields=email&access_token=TOKEN";

    private final CloseableHttpClient httpclient;
    private final ObjectReader facebookResponseReader = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .reader(FacebookVerifyResponse.class);

    public FacebookLoginCheck() {
        this.httpclient = HttpClients.createDefault();
    }

    @SuppressWarnings("unchecked")
    public void verify(String username, String token) throws Exception {
        HttpGet httpGet = new HttpGet(URL.replace("TOKEN", token));

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            String msg = EntityUtils.toString(entity);
            if (response.getStatusLine().getStatusCode() == 200) {
                FacebookVerifyResponse facebookVerifyResponse = facebookResponseReader.readValue(msg);
                if (!username.equals(facebookVerifyResponse.email)) {
                    throw new IllegalArgumentException("Token is invalid. Email is wrong.");
                }
            } else {
                EntityUtils.consume(entity);
                throw new IOException(msg);
            }
        } finally {
            httpGet.releaseConnection();
        }
    }

    private static class FacebookVerifyResponse {
        public String id;
        public String email;
    }

}
