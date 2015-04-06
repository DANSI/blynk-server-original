package cc.blynk.server.workers.notifications;

import cc.blynk.common.utils.Config;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.exceptions.ServerBusyException;
import cc.blynk.server.notifications.mail.MailSender;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.server.notifications.twitter.model.TwitterAccessToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger log = LogManager.getLogger(NotificationsProcessor.class);

    private static final int NOTIFICATIONS_PROCESSORS = 10;

    private final TwitterWrapper twitterWrapper;
    private final MailSender mailSender;
    private final ThreadPoolExecutor executor;

    public NotificationsProcessor(int maxQueueSize) {
        this.twitterWrapper = new TwitterWrapper();
        this.mailSender = new MailSender(new ServerProperties(Config.MAIL_PROPERTIES_FILENAME));
        this.executor = new ThreadPoolExecutor(
                NOTIFICATIONS_PROCESSORS, NOTIFICATIONS_PROCESSORS,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
    }

    public void mail(String to, String subj, String body, int msgId) {
        try {
            executor.execute(mailSender.produceSendMailTask(to, subj, body));
        } catch (RejectedExecutionException e) {
            throw new ServerBusyException(msgId);
        }
    }

    public void twit(TwitterAccessToken twitterAccessToken, String body, int msgId) {
        try {
            executor.execute(twitterWrapper.produceSendTwitTask(twitterAccessToken, body));
        } catch (RejectedExecutionException e) {
            throw new ServerBusyException(msgId);
        }
    }

    public void stop() {
        executor.shutdown();
    }

}
