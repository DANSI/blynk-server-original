package cc.blynk.server.core.processors;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.others.webhook.Header;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.protocol.enums.Command;
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
import java.util.regex.Matcher;

import static cc.blynk.server.core.protocol.enums.Command.WEB_HOOKS;
import static cc.blynk.utils.StringUtils.DATETIME_PATTERN;
import static cc.blynk.utils.StringUtils.DEVICE_OWNER_EMAIL;
import static cc.blynk.utils.StringUtils.GENERIC_PLACEHOLDER;
import static cc.blynk.utils.StringUtils.PIN_PATTERN;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_0;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_1;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_2;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_3;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_4;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_5;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_6;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_7;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_8;
import static cc.blynk.utils.StringUtils.PIN_PATTERN_9;

/**
 * Handles all webhooks logic.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.09.16.
 */
public class WebhookProcessor extends NotificationBase {

    private static final Logger log = LogManager.getLogger(WebhookProcessor.class);
    private static final String CONTENT_TYPE = "Content-Type";

    private final AsyncHttpClient httpclient;
    private final GlobalStats globalStats;
    private final int responseSizeLimit;
    private final String email;
    private final int webhookFailureLimit;

    public WebhookProcessor(DefaultAsyncHttpClient httpclient,
                            long quotaFrequencyLimit,
                            int responseSizeLimit,
                            int failureLimit,
                            GlobalStats stats, String email) {
        super(quotaFrequencyLimit);
        this.httpclient = httpclient;
        this.globalStats = stats;
        this.responseSizeLimit = responseSizeLimit;
        this.email = email;
        this.webhookFailureLimit = failureLimit;
    }

    public void process(User user, Session session, DashBoard dash, int deviceId, short pin,
                        PinType pinType, String triggerValue, long now) {
        WebHook webhook = dash.findWebhookByPin(deviceId, pin, pinType);
        if (webhook == null) {
            return;
        }

        checkIfNotificationQuotaLimitIsNotReached(now);

        if (webhook.isNotFailed(webhookFailureLimit) && webhook.url != null) {
            process(user, session, dash.id, deviceId, webhook, triggerValue);
        }
    }

    private void process(User user, Session session, int dashId, int deviceId,  WebHook webHook, String triggerValue) {
        String newUrl = format(webHook.url, triggerValue, user.email);

        if (!WebHook.isValidUrl(newUrl)) {
            return;
        }

        BoundRequestBuilder builder;
        try {
            builder = httpclient.prepare(webHook.method.name(), newUrl);
        } catch (NumberFormatException nfe) {
            //this is known possible error due to malformed input
            //https://github.com/blynkkk/blynk-server/issues/1001
            log.debug("Error during webhook initialization.", nfe);
            return;
        } catch (IllegalArgumentException iae) {
            String error = iae.getMessage();
            if (error != null && error.contains("missing scheme")) {
                //this is known possible error due to malformed input
                log.debug("Error during webhook initialization.", iae);
                return;
            } else {
                throw iae;
            }
        }

        if (webHook.headers != null) {
            for (Header header : webHook.headers) {
                if (header.isValid()) {
                    builder.setHeader(header.name, header.value);
                    if (webHook.body != null && !webHook.body.isEmpty()) {
                        if (CONTENT_TYPE.equals(header.name)) {
                            String newBody = format(webHook.body, triggerValue, user.email);
                            log.trace("Webhook formatted body : {}", newBody);
                            builder.setBody(newBody);
                        }
                    }
                }
            }
        }

        log.trace("Sending webhook. {}", webHook);
        builder.execute(new AsyncCompletionHandler<Response>() {

            private int length = 0;

            @Override
            public State onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                length += content.length();

                if (length > responseSizeLimit) {
                    log.warn("Response from webhook is too big for {}. Skipping. Size : {}", email, length);
                    return State.ABORT;
                }
                return super.onBodyPartReceived(content);
            }

            @Override
            public Response onCompleted(Response response) {
                if (isValidResponseCode(response.getStatusCode())) {
                    webHook.failureCounter = 0;
                    if (response.hasResponseBody()) {
                        //todo could be optimized with response.getResponseBodyAsByteBuffer()
                        String body = DataStream.makeHardwareBody(webHook.pinType, webHook.pin,
                                response.getResponseBody(CharsetUtil.UTF_8));
                        log.trace("Sending webhook to hardware. {}", body);
                        session.sendMessageToHardware(dashId, Command.HARDWARE, 888, body, deviceId);
                    }
                } else {
                    webHook.failureCounter++;
                    log.debug("Error sending webhook for {}. Code {}.", email, response.getStatusCode());
                    if (log.isDebugEnabled()) {
                        log.debug("Reason {}", response.getResponseBody());
                    }
                }

                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                webHook.failureCounter++;
                log.debug("Error sending webhook for {}.", email);
                if (log.isDebugEnabled()) {
                    log.debug("Reason {}", t.getMessage());
                }
            }
        });
        globalStats.mark(WEB_HOOKS);
    }

    private static boolean isValidResponseCode(int responseCode) {
        switch (responseCode) {
            case 200:
            case 201:
            case 202:
            case 204:
            case 302:
                return true;
            default:
                return false;
        }
    }

    private static String format(String data, String triggerValue, String ownerEmail) {
        //this is an ugly hack to make it work with Blynk HTTP API.
        String quotedValue = Matcher.quoteReplacement(triggerValue);
        data = PIN_PATTERN.matcher(data).replaceFirst(quotedValue);

        String[] splitted = quotedValue.split(StringUtils.BODY_SEPARATOR_STRING);
        switch (splitted.length) {
            case 10 :
                data = PIN_PATTERN_9.matcher(data).replaceFirst(splitted[9]);
            case 9 :
                data = PIN_PATTERN_8.matcher(data).replaceFirst(splitted[8]);
            case 8 :
                data = PIN_PATTERN_7.matcher(data).replaceFirst(splitted[7]);
            case 7 :
                data = PIN_PATTERN_6.matcher(data).replaceFirst(splitted[6]);
            case 6 :
                data = PIN_PATTERN_5.matcher(data).replaceFirst(splitted[5]);
            case 5 :
                data = PIN_PATTERN_4.matcher(data).replaceFirst(splitted[4]);
            case 4 :
                data = PIN_PATTERN_3.matcher(data).replaceFirst(splitted[3]);
            case 3 :
                data = PIN_PATTERN_2.matcher(data).replaceFirst(splitted[2]);
            case 2 :
                data = PIN_PATTERN_1.matcher(data).replaceFirst(splitted[1]);
            case 1 :
                data = PIN_PATTERN_0.matcher(data).replaceFirst(splitted[0]);
            default :
                data = GENERIC_PLACEHOLDER.matcher(data).replaceFirst(quotedValue);
                data = DATETIME_PATTERN.matcher(data).replaceFirst(Instant.now().toString());
                data = DEVICE_OWNER_EMAIL.matcher(data).replaceFirst(ownerEmail);
        }
        return data;
    }
}
