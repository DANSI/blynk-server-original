package cc.blynk.server.workers.notifications;

import cc.blynk.common.utils.Config;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.exceptions.ServerBusyException;
import cc.blynk.server.notifications.mail.MailSender;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public class NotificationsProcessor {

    private static final int NOTIFICATIONS_PROCESSORS = 10;

    private final TwitterWrapper twitterWrapper;
    private final MailSender mailSender;
    private final GCMWrapper gcmWrapper;
    private final ThreadPoolExecutor executor;

    public NotificationsProcessor(int maxQueueSize) {
        this.twitterWrapper = new TwitterWrapper();
        this.mailSender = new MailSender(new ServerProperties(Config.MAIL_PROPERTIES_FILENAME));
        this.gcmWrapper = new GCMWrapper();
        this.executor = new ThreadPoolExecutor(
                NOTIFICATIONS_PROCESSORS, NOTIFICATIONS_PROCESSORS,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
    }

    public void mail(String to, String subj, String body, int msgId) {
        try {
            executor.execute(mailSender.produce(to, subj, body));
        } catch (RejectedExecutionException e) {
            throw new ServerBusyException(msgId);
        }
    }

    public void twit(String token, String secret, String body, int msgId) {
        try {
            executor.execute(twitterWrapper.produce(token, secret, body));
        } catch (RejectedExecutionException e) {
            throw new ServerBusyException(msgId);
        }
    }

    public void push(String token, String body, int msgId) {
        try {
            Map<String, String> data = new HashMap<String, String>() {
                {
                    put("message", body);
                }
            };
            executor.execute(gcmWrapper.produce(token, data));
        } catch (RejectedExecutionException e) {
            throw new ServerBusyException(msgId);
        }
    }

    public void stop() {
        executor.shutdown();
    }

}
