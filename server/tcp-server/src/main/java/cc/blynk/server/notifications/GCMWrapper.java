package cc.blynk.server.notifications;

import cc.blynk.common.utils.ServerProperties;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMWrapper {

    private static final String filePropertiesName = "gcm.properties";

    private final String API_KEY;
    private final CloseableHttpClient httpclient;
    private final URI gcmURI;

    public GCMWrapper() {
        ServerProperties props = new ServerProperties(filePropertiesName);
        this.API_KEY = "key=" + props.getProperty("gcm.api.key");
        this.httpclient = HttpClients.createDefault();
        this.gcmURI = URI.create(props.getProperty("gcm.server"));
    }

    public void send(String to, String body) throws Exception {
        HttpPost httpPost = new HttpPost(gcmURI);
        httpPost.setHeader("Authorization", API_KEY);
        httpPost.setEntity(new StringEntity(new GCMMessage(to, body).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                HttpEntity entity = response.getEntity();
                String errorMsg = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
                throw new Exception(errorMsg);
            }
        }
    }
}
