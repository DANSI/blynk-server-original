package cc.blynk.server.notifications.twitter;

import cc.blynk.server.notifications.twitter.model.TwitterAccessToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/6/2015.
 */
public class TwitterWrapper {

    private static final Logger log = LogManager.getLogger(TwitterWrapper.class);

    // The factory instance is re-useable and thread safe.
    private final TwitterFactory factory = new TwitterFactory();

    public Runnable produceSendTwitTask(TwitterAccessToken twitterAccessToken, String message) {
        return produceSendTwitTask(twitterAccessToken.getToken(), twitterAccessToken.getTokenSecret(), message);
    }

    protected Runnable produceSendTwitTask(String token, String tokenSecret, String message) {
        return () -> {
            try {
                AccessToken accessToken = new AccessToken(token, tokenSecret);
                Twitter twitter = factory.getInstance();
                twitter.setOAuthAccessToken(accessToken);

                twitter.updateStatus(message);
            } catch (TwitterException e) {
                log.error("Error sending twit.");
            }

        };
    }

}
