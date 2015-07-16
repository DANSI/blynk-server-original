package cc.blynk.server.notifications;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.utils.JsonParser;
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
        String server = props.getProperty("gcm.server");
        if (server == null) {
            this.gcmURI = null;
        } else {
            this.gcmURI = URI.create(server);
        }
    }

    public void send(GCMMessage messageBase) throws Exception {
        if (gcmURI == null) {
            throw new Exception("Error sending push. Google cloud messaging properties not provided.");
        }

        HttpPost httpPost = new HttpPost(gcmURI);
        httpPost.setHeader("Authorization", API_KEY);
        httpPost.setEntity(new StringEntity(messageBase.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            String errorMsg = EntityUtils.toString(entity);
            if (response.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consume(entity);
                throw new Exception(errorMsg);
            } else {
                GCMResponseMessage gcmResponseMessage = JsonParser.parseGCMResponse(errorMsg);
                if (gcmResponseMessage.failure == 1) {
                    if (gcmResponseMessage.results != null && gcmResponseMessage.results.length > 0) {
                        throw new Exception("Error sending push. Problem : " + gcmResponseMessage.results[0].error);
                    } else {
                        throw new Exception("Error sending push. Token : " + messageBase.getToken());
                    }
                }
            }
        }
    }
}
