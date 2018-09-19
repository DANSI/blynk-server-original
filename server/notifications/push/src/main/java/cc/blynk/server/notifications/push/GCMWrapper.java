package cc.blynk.server.notifications.push;

import cc.blynk.utils.properties.GCMProperties;
import cc.blynk.utils.properties.Placeholders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMWrapper {

    private static final Logger log = LogManager.getLogger(GCMWrapper.class);

    private final String apiKey;
    private final AsyncHttpClient httpclient;
    private final String gcmURI;
    private final String title;
    private final ObjectReader gcmResponseReader = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readerFor(GCMResponseMessage.class);

    public GCMWrapper(GCMProperties props, AsyncHttpClient httpclient, String productName) {
        this.apiKey = "key=" + props.getGCMApiKey();
        this.httpclient = httpclient;
        this.gcmURI = props.getGCMServer();

        String title = props.getNotificationTitle();
        this.title = title.replace(Placeholders.PRODUCT_NAME, productName);
    }

    private static void processError(String errorMessage, Map<String, String> tokens, String uid) {
        log.error("Error sending push. Reason {}", errorMessage);
        clean(errorMessage, tokens, uid);
    }

    private static void clean(String errorMessage, Map<String, String> tokens, String uid) {
        if (errorMessage != null && errorMessage.contains("NotRegistered")) {
            log.error("Removing invalid token. UID {}", uid);
            tokens.remove(uid);
        }
    }

    public void send(GCMMessage messageBase, final Map<String, String> tokens, final String uid) {
        if (gcmURI == null) {
            log.error("Error sending push. Google cloud messaging properties not provided.");
            return;
        }

        String message;
        try {
            messageBase.setTitle(title);
            message = messageBase.toJson();
        } catch (JsonProcessingException e) {
            log.error("Error sending push. Wrong message format.");
            return;
        }

        httpclient.preparePost(gcmURI).setHeader("Authorization", apiKey)
                .setHeader(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
                .setBody(message)
                .execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        if (response.getStatusCode() == HttpResponseStatus.OK.code()) {
                            GCMResponseMessage gcmResponseMessage =
                                    gcmResponseReader.readValue(response.getResponseBody());
                            if (gcmResponseMessage.failure == 1) {
                                String errorMessage =
                                        gcmResponseMessage.results != null && gcmResponseMessage.results.length > 0
                                                ? gcmResponseMessage.results[0].error
                                                : messageBase.getToken();
                                processError(errorMessage, tokens, uid);
                            }
                            return response;
                        }

                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        processError(t.getMessage(), tokens, uid);
                    }
                });

    }

}
