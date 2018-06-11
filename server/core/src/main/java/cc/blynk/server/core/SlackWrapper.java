package cc.blynk.server.core;

import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.utils.properties.SlackProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

public class SlackWrapper {

    private static final Logger log = LogManager.getLogger(SlackWrapper.class);

    private final String webHookUrl;
    private final String channel;
    private final AsyncHttpClient asyncHttpClient;
    private final String region;

    public SlackWrapper(SlackProperties slackProperties, AsyncHttpClient asyncHttpClient, String region) {
        this.webHookUrl = slackProperties.getWebHookUrl();
        this.channel = slackProperties.getChannelName();
        this.asyncHttpClient = asyncHttpClient;
        this.region = region;
        if (isValid()) {
            log.info("Slack integration is enabled.");
        }
    }

    private boolean isValid() {
        return webHookUrl != null && !webHookUrl.isEmpty() && channel != null && !channel.isEmpty();
    }

    private static String buildBody(String email, String appVersion, double price) {
        return "$" + price + " from " + email + " (" + appVersion + ")";
    }

    public void reportPurchase(String email, String appVersion, double price) {
        //do not track small purchases, there are too many of them.
        if (!isValid()) {
            return;
        }

        String message = buildBody(email, appVersion, price);
        SlackPurchaseMessage slackPurchaseMessage = new SlackPurchaseMessage(channel, region, message);

        asyncHttpClient.preparePost(webHookUrl)
                .setBody(slackPurchaseMessage.toString())
                .execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) {
                        if (response.getStatusCode() != 200) {
                            log.debug("Error insert purchase into slack");
                        }
                        return response;
                    }
                });
    }

    private static class SlackPurchaseMessage {

        public final String channel;

        public final String username;

        public final String text;

        @JsonProperty("icon_emoji")
        public final String iconEmoji;

        SlackPurchaseMessage(String channel, String region, String text) {
            this.channel = channel;
            this.username = region;
            this.text = text;
            this.iconEmoji = ":moneybag:";
        }

        @Override
        public String toString() {
            return JsonParser.toJson(this);
        }
    }
}
