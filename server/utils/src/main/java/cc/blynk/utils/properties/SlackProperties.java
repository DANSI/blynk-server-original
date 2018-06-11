package cc.blynk.utils.properties;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.01.17.
 */
public class SlackProperties extends BaseProperties {

    private static final String SLACK_PROPERTIES_FILENAME = "slack.properties";

    public SlackProperties(Map<String, String> cmdProperties) {
        super(cmdProperties, SLACK_PROPERTIES_FILENAME);
    }

    public String getWebHookUrl() {
        return getProperty("webhook.url");
    }

    public String getChannelName() {
        String channel = getProperty("channel");
        if (channel != null && !channel.startsWith("#")) {
            return "#" + channel;
        }
        return channel;
    }

}
