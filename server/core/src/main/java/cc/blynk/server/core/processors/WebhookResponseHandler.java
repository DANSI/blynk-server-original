package cc.blynk.server.core.processors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Response;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.09.16.
 */
final class WebhookResponseHandler extends AsyncCompletionHandler<Response> {

    private static final Logger log = LogManager.getLogger(WebhookResponseHandler.class);

    @Override
    public Response onCompleted(Response response) throws Exception {
        if (response.getStatusCode() == 200) {
            return response;
        }

        log.error("Error sending webhook. Reason {}", response.getResponseBody());
        return response;
    }

    @Override
    public void onThrowable(Throwable t) {
        log.error("Error sending webhook. Reason {}", t.getMessage());
    }

}
