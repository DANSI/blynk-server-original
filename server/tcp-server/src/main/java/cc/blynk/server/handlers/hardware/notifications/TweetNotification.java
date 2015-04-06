package cc.blynk.server.handlers.hardware.notifications;

import cc.blynk.server.notifications.twitter.model.TwitterAccessToken;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public class TweetNotification extends NotificationBase {

    public TwitterAccessToken accessToken;

    public TweetNotification(TwitterAccessToken accessToken, String body, int msgId) {
        super(body, msgId);
        this.accessToken = accessToken;
    }
}
