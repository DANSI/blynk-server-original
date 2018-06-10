package cc.blynk.server.db.model;

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

    public void reportPurchase(String email, double price) {
        //do not track small purchases, there are too many of them.
        if (price < 3.0D || !isValid()) {
            return;
        }
        SlackPurchaseMessage slackPurchaseMessage = new SlackPurchaseMessage(channel, region, email, price);
        asyncHttpClient
                .preparePost(webHookUrl)
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

        public SlackPurchaseMessage(String channel, String region, String email, double price) {
            this.channel = channel;
            this.username = region;
            this.text = buildBody(email, price);
            this.iconEmoji = ":moneybag:";
        }

        private static String buildBody(String email, double price) {
            return "User " + email + " made " + price + "$ purchase.";
        }

        @Override
        public String toString() {
            return JsonParser.toJson(this);
        }
    }
}
