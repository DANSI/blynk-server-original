package cc.blynk.utils.properties;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class TwitterProperties extends BaseProperties {

    public static final String TWITTER_PROPERTIES_FILENAME = "twitter4j.properties";

    public TwitterProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, TWITTER_PROPERTIES_FILENAME);
    }

    public String getConsumerKey() {
        return getProperty("oauth.consumerKey");
    }

    public String getConsumerSecret() {
        return getProperty("oauth.consumerSecret");
    }
}
