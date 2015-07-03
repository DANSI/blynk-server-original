package cc.blynk.server.workers.notifications;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.utils.Config;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.notifications.GCMWrapper;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

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
    private Logger log = LogManager.getLogger(NotificationsProcessor.class);

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
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(produce(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId);
            }
        });
    }

    public void twit(Channel channel, String token, String secret, String body, int msgId) {
        executor.execute(() -> {
            try {
                twitterWrapper.send(token, secret, body);
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(produce(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId);
            }
        });
    }

    public void push(Channel channel, String token, String body, int msgId) {
        executor.execute(() -> {
            try {
                gcmWrapper.send(token, body);
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(produce(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId);
            }
        });
    }


    public void push(User user, String token, String body) {
        executor.execute(() -> {
            try {
                gcmWrapper.send(token, body);
            } catch (Exception e) {
                log(user, e.getMessage());
            }
        });
    }

    public void stop() {
        executor.shutdown();
    }

    private void log(Channel channel, String errorMessage, int msgId) {
        User user = channel.attr(ChannelState.USER).get();
        if (user != null) {
            ThreadContext.put("user", user.getName());
            log.error("Error sending notification. {}", errorMessage);
            ThreadContext.clearMap();
        }
        channel.eventLoop().execute(() -> {
            channel.writeAndFlush(new ResponseMessage(msgId, Command.RESPONSE, Response.NOTIFICATION_EXCEPTION));
        });
    }

    private void log(User user, String errorMessage) {
        if (user != null) {
            ThreadContext.put("user", user.getName());
            log.error("Error sending notification. {}", errorMessage);
            ThreadContext.clearMap();
        }
    }

}
