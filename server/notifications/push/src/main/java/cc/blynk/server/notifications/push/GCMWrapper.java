package cc.blynk.server.notifications.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.Properties;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMWrapper {

    public static final String GCM_PROPERTIES_FILENAME = "gcm.properties";

    private final String API_KEY;
    private final CloseableHttpClient httpclient;
    private final URI gcmURI;
    private final ObjectReader gcmResponseReader = new ObjectMapper().reader(GCMResponseMessage.class);

    public GCMWrapper(Properties props) {
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
        httpPost.setEntity(new StringEntity(messageBase.toJson(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            String errorMsg = EntityUtils.toString(entity);
            if (response.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consume(entity);
                throw new Exception(errorMsg);
            } else {
                GCMResponseMessage gcmResponseMessage = gcmResponseReader.readValue(errorMsg);
                if (gcmResponseMessage.failure == 1) {
                    if (gcmResponseMessage.results != null && gcmResponseMessage.results.length > 0) {
                        throw new Exception("Error sending push. Problem : " + gcmResponseMessage.results[0].error);
                    } else {
                        throw new Exception("Error sending push. Token : " + messageBase.getToken());
                    }
                }
            }
        } finally {
            httpPost.releaseConnection();
        }
    }

}
