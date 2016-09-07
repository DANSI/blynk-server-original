package cc.blynk.server.core.processors;

import cc.blynk.server.core.model.widgets.others.webhook.Header;
import cc.blynk.server.core.model.widgets.others.webhook.SupportedWebhookMethod;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import io.netty.channel.EventLoopGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;

/**
 * Handles all webhooks logic.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.09.16.
 */
public class WebhookProcessor {

    private static final Logger log = LogManager.getLogger(WebhookProcessor.class);

    private final AsyncHttpClient httpclient;

    public WebhookProcessor(EventLoopGroup workerGroup) {
        this.httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setEventLoopGroup(workerGroup)
                        .setKeepAlive(false)
                        .build()
        );
    }

    //todo could be optimized
    public void process(WebHook webHook, String triggerValue) {
        if (!webHook.isValid()) {
            return;
        }
        String newUrl = webHook.url.replace("%s", triggerValue);
        BoundRequestBuilder builder = buildRequestMethod(webHook.method, newUrl);

        if (webHook.headers != null) {
            for (Header header : webHook.headers) {
                if (header.isValid()) {
                    builder.setHeader(header.name, header.value);
                    if (header.name.equals("Content-Type") && webHook.body != null && !webHook.body.equals("")) {
                        String newBody = webHook.body.replace("%s", triggerValue);
                        buildRequestBody(builder, header.value, newBody);
                    }
                }
            }
        }

        builder.execute(new ResponseHandler());
    }

    private BoundRequestBuilder buildRequestBody(BoundRequestBuilder builder, String header, String body) {
        switch (header) {
            case "application/json" :
                builder.setBody(body);
                break;
            default :
                throw new IllegalArgumentException("Unsupported content-type for webhook.");
        }

        return builder;
    }

    private BoundRequestBuilder buildRequestMethod(SupportedWebhookMethod method, String url) {
        switch (method) {
            case GET :
                return httpclient.prepareGet(url);
            case POST :
                return httpclient.preparePost(url);
            case PUT :
                return httpclient.preparePut(url);
            case DELETE :
                return httpclient.prepareDelete(url);
            default :
                throw new IllegalArgumentException("Unsupported method type for webhook.");
        }
    }

    private static final class ResponseHandler extends AsyncCompletionHandler<Response> {

        @Override
        public Response onCompleted(org.asynchttpclient.Response response) throws Exception {
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

    };

}
