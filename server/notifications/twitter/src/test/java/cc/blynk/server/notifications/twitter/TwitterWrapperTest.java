package cc.blynk.server.notifications.twitter;

import org.junit.Ignore;
import org.junit.Test;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/7/2015.
 */
public class TwitterWrapperTest {

    @Test
    @Ignore
    public void testTweet() {
        String token = "PUT_YOUR_TOKEN_HERE";
        String tokenSecret = "PUT_YOUR_TOKEN_SECRET_HERE";
        new TwitterWrapper().produce(token, tokenSecret, "Hello444").run();
    }

}
