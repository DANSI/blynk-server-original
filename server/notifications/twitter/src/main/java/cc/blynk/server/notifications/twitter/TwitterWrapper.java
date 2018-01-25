package cc.blynk.server.notifications.twitter;

import cc.blynk.utils.properties.TwitterProperties;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.asynchttpclient.oauth.ConsumerKey;
import org.asynchttpclient.oauth.OAuthSignatureCalculator;
import org.asynchttpclient.oauth.RequestToken;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/6/2015.
 */
public class TwitterWrapper {

    private static final String TWITTER_UPDATE_STATUS_URL = "https://api.twitter.com/1.1/statuses/update.json";
    private final ConsumerKey consumerKey;
    private final AsyncHttpClient asyncHttpClient;

    public TwitterWrapper(TwitterProperties twitterProperties, AsyncHttpClient asyncHttpClient) {
        this.consumerKey = new ConsumerKey(
                twitterProperties.getConsumerKey(),
                twitterProperties.getConsumerSecret()
        );
        this.asyncHttpClient = asyncHttpClient;
    }

    public void send(String token, String secret, String message,
                     AsyncCompletionHandler<Response> handler) {
        asyncHttpClient
                .preparePost(TWITTER_UPDATE_STATUS_URL)
                .addQueryParam("status", message)
                .setSignatureCalculator(new OAuthSignatureCalculator(consumerKey, new RequestToken(token, secret)))
                .execute(handler);
    }
}
