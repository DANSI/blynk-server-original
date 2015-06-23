package cc.blynk.server.workers.notifications;

import cc.blynk.common.utils.Config;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.exceptions.NotificationException;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import io.netty.channel.Channel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public class NotificationsProcessor {

    //todo move to properties
    private static final int NOTIFICATIONS_PROCESSORS = 5;

    private final TwitterWrapper twitterWrapper;
    private final MailWrapper mailWrapper;
    private final GCMWrapper gcmWrapper;
    private final ThreadPoolExecutor executor;

    public NotificationsProcessor(int maxQueueSize) {
        this.twitterWrapper = new TwitterWrapper();
        this.mailWrapper = new MailWrapper(new ServerProperties(Config.MAIL_PROPERTIES_FILENAME));
        this.gcmWrapper = new GCMWrapper();
        this.executor = new ThreadPoolExecutor(
                NOTIFICATIONS_PROCESSORS, NOTIFICATIONS_PROCESSORS,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
    }

    public void mail(Channel channel, String to, String subj, String body, int msgId) {
        executor.execute(() -> {
            try {
                mailWrapper.send(to, subj, body, null);
            } catch (Exception e) {
                channel.eventLoop().execute(() -> {
                    throw new NotificationException("Error sending email. " + e.getMessage(), msgId);
                });
            }
        });
    }

    public void twit(Channel channel, String token, String secret, String body, int msgId) {
        executor.execute(() -> {
            try {
                twitterWrapper.send(token, secret, body);
            } catch (Exception e) {
                channel.eventLoop().execute(() -> {
                    throw new NotificationException("Error sending tweet. " + e.getMessage(), msgId);
                });
            }
        });
    }

    public void push(Channel channel, String token, String body, int msgId) {
        executor.execute(() -> {
            try {
                gcmWrapper.send(token, body);
            } catch (Exception e) {
                channel.eventLoop().execute(() -> {
                    throw new NotificationException("Error sending push. " + e.getMessage(), msgId);
                });
            }
        });
    }

    public void stop() {
        executor.shutdown();
    }

}
