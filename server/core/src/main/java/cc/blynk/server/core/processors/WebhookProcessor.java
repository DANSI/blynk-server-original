package cc.blynk.server.core.processors;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.others.webhook.Header;
import cc.blynk.server.core.model.widgets.others.webhook.SupportedWebhookMethod;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import static cc.blynk.utils.StringUtils.PIN_PATTERN;

/**
 * Handles all webhooks logic.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.09.16.
 */
public class WebhookProcessor extends NotificationBase {

    private static final Logger log = LogManager.getLogger(WebhookProcessor.class);

    private final AsyncHttpClient httpclient;

    public WebhookProcessor(DefaultAsyncHttpClient httpclient, long quotaFrequencyLimit) {
        super(quotaFrequencyLimit);
        this.httpclient = httpclient;
    }

    public void process(DashBoard dash, byte pin, PinType pinType, String triggerValue) {
        Widget widget = dash.findWidgetByPin(pin, pinType);
        if (widget == null) {
            return;
        }
        if (widget instanceof WebHook) {
            try {
                checkIfNotificationQuotaLimitIsNotReached();
            } catch (QuotaLimitException qle) {
                log.debug("Webhook quota limit reached. Ignoring hook.");
                return;
            }
            process((WebHook) widget, triggerValue);
        }
    }

    public void process(WebHook webHook, String triggerValue) {
        if (!webHook.isValid()) {
            return;
        }

        String newUrl = prepareUrl(webHook.url, triggerValue);

        BoundRequestBuilder builder = buildRequestMethod(webHook.method, newUrl);

        if (webHook.headers != null) {
            for (Header header : webHook.headers) {
                if (header.isValid()) {
                    builder.setHeader(header.name, header.value);
                    if (webHook.body != null && !webHook.body.equals("")) {
                        if (header.name.equals("Content-Type")) {
                            String newBody = webHook.body
                                    .replace("%s", triggerValue)
                                    .replace(PIN_PATTERN, triggerValue);
                                buildRequestBody(builder, header.value, newBody);
                            }
                    }
                }
            }
        }

        builder.execute(new ResponseHandler());
    }

    private String prepareUrl(String url, String triggerValue) {
        //this is an ugly hack to make it work with Blynk HTTP API.
        if (!url.toLowerCase().contains("/pin/v")) {
            url = url.replace(PIN_PATTERN, triggerValue);
        }
        return url.replace("%s", triggerValue);
    }

    private BoundRequestBuilder buildRequestBody(BoundRequestBuilder builder, String header, String body) {
        switch (header) {
            case "application/json" :
            case "text/plain" :
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

    }

}
