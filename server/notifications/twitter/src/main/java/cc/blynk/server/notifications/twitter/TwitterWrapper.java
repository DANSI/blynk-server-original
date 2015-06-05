package cc.blynk.server.notifications.twitter;

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

    public Runnable produceSendTwitTask(String token, String secret, String message) {
        return () -> {
            try {
                AccessToken accessToken = new AccessToken(token, secret);
                Twitter twitter = factory.getInstance();
                twitter.setOAuthAccessToken(accessToken);

                twitter.updateStatus(message);
            } catch (TwitterException e) {
                log.error("Error sending twit. Reason : {}", e.getMessage());
            }

        };
    }

}
