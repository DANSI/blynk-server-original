package cc.blynk.server.core.processors;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.webhook.Header;
import cc.blynk.server.core.model.widgets.others.webhook.SupportedWebhookMethod;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.utils.StringUtils;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Response;

import java.time.Instant;

import static cc.blynk.server.core.protocol.enums.Command.WEB_HOOKS;
import static cc.blynk.utils.StringUtils.DATETIME_PATTERN;
import static cc.blynk.utils.StringUtils.PIN_PATTERN;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_0;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_1;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_2;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_3;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_4;

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
    private final GlobalStats globalStats;
    private final int responseSizeLimit;
    private final String username;

    public WebhookProcessor(DefaultAsyncHttpClient httpclient, long quotaFrequencyLimit, int responseSizeLimit,
                            GlobalStats stats, String username) {
        super(quotaFrequencyLimit);
        this.httpclient = httpclient;
        this.globalStats = stats;
        this.responseSizeLimit = responseSizeLimit;
        this.username = username;
    }

    public void process(Session session, DashBoard dash, int deviceId, byte pin, PinType pinType, String triggerValue) {
        WebHook widget = dash.findWebhookByPin(deviceId, pin, pinType);
        if (widget == null) {
            return;
        }

        try {
            checkIfNotificationQuotaLimitIsNotReached();
        } catch (QuotaLimitException qle) {
            log.debug("Webhook quota limit reached. Ignoring hook.");
            return;
        }
        process(session, dash.id, deviceId, widget, triggerValue);
    }

    private void process(Session session, int dashId, int deviceId,  WebHook webHook, String triggerValue) {
        if (!webHook.isValid()) {
            return;
        }

        String newUrl = format(webHook.url, triggerValue, false);

        BoundRequestBuilder builder = buildRequestMethod(webHook.method, newUrl);
        if (webHook.headers != null) {
            for (Header header : webHook.headers) {
                if (header.isValid()) {
                    builder.setHeader(header.name, header.value);
                    if (webHook.body != null && !webHook.body.equals("")) {
                        if (header.name.equals("Content-Type")) {
                            String newBody = format(webHook.body, triggerValue, true);
                                buildRequestBody(builder, header.value, newBody);
                            }
                    }
                }
            }
        }

        log.trace("Sending webhook. ", webHook);
        builder.execute(new AsyncCompletionHandler<Response>() {

            private int length = 0;

            @Override
            public State onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                length += content.length();

                if (length > responseSizeLimit) {
                    log.warn("Response from webhook is too big for {}. Skipping. Size : {}", username, length);
                    return State.ABORT;
                }
                return super.onBodyPartReceived(content);
            }

            @Override
            public Response onCompleted(Response response) throws Exception {
                if (response.getStatusCode() == 200 || response.getStatusCode() == 302) {
                    webHook.failureCounter = 0;
                    if (response.hasResponseBody()) {
                        //todo could be optimized
                        String body = Pin.makeHardwareBody(webHook.pinType, webHook.pin, response.getResponseBody(CharsetUtil.UTF_8));
                        log.trace("Sending webhook to hardware. {}", body);
                        session.sendMessageToHardware(dashId, Command.HARDWARE, 888, body, deviceId);
                    }
                } else {
                    webHook.failureCounter++;
                    log.error("Error sending webhook for {}. Code {}, Reason {}", username, response.getStatusCode(), response.getResponseBody());
                }

                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                webHook.failureCounter++;
                log.error("Error sending webhook for {}. Reason {}", username, t.getMessage());
            }
        });
        globalStats.mark(WEB_HOOKS);
    }

    //todo this is very straightforward solution. should be optimized.
    private String format(String data, String triggerValue, boolean doBlynkCheck) {
        //this is an ugly hack to make it work with Blynk HTTP API.
        if (doBlynkCheck || !data.toLowerCase().contains("/pin/v")) {
            data = data.replace(PIN_PATTERN, triggerValue);
        }
        data = data.replace("%s", triggerValue);
        String[] splitted = triggerValue.split(StringUtils.BODY_SEPARATOR_STRING);
        switch (splitted.length) {
            case 5 :
                data = data.replace(PIN_PATTERN_4, splitted[4]);
            case 4 :
                data = data.replace(PIN_PATTERN_3, splitted[3]);
            case 3 :
                data = data.replace(PIN_PATTERN_2, splitted[2]);
            case 2 :
                data = data.replace(PIN_PATTERN_1, splitted[1]);
            case 1 :
                data = data.replace(PIN_PATTERN_0, splitted[0]);
        }

        data = data.replace(DATETIME_PATTERN, Instant.now().toString());

        return data;
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

}
