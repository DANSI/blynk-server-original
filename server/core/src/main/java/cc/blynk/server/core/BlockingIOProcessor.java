package cc.blynk.server.core;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetGraphDataBinaryMessage;
import cc.blynk.server.core.reporting.GraphPinRequest;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMMessage;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.ios.IOSGCMMessage;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.utils.Config;
import cc.blynk.utils.ServerProperties;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.ByteUtils.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public class BlockingIOProcessor {

    //todo move to properties
    private static final int NOTIFICATIONS_PROCESSORS = 5;
    private final TwitterWrapper twitterWrapper;
    private final MailWrapper mailWrapper;
    private final GCMWrapper gcmWrapper;
    private final ReportingDao reportingDao;
    private final ThreadPoolExecutor executor;
    private final Logger log = LogManager.getLogger(BlockingIOProcessor.class);
    public volatile String tokenBody;

    public BlockingIOProcessor(int maxQueueSize, String tokenBody, ReportingDao reportingDao) {
        this.twitterWrapper = new TwitterWrapper();
        this.mailWrapper = new MailWrapper(new ServerProperties(Config.MAIL_PROPERTIES_FILENAME));
        this.gcmWrapper = new GCMWrapper(new ServerProperties(GCMWrapper.GCM_PROPERTIES_FILENAME));
        this.reportingDao = reportingDao;
        this.executor = new ThreadPoolExecutor(
                NOTIFICATIONS_PROCESSORS, NOTIFICATIONS_PROCESSORS,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
        this.tokenBody = tokenBody;
    }

    public static User getStateUser(Channel channel) {
        BaseSimpleChannelInboundHandler handler = channel.pipeline().get(BaseSimpleChannelInboundHandler.class);
        return handler == null ? null : handler.state.user;
    }

    public void readGraphData(Channel channel, String name, GraphPinRequest[] requestedPins, int msgId) {
        executor.execute(() -> {
            try {
                byte[][] data = reportingDao.getAllFromDisk(name, requestedPins, msgId);
                byte[] compressed = compress(requestedPins[0].dashId, data, msgId);

                log.trace("Sending getGraph response. ");
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(new GetGraphDataBinaryMessage(msgId, compressed));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NO_DATA_EXCEPTION);
            }
        });
    }

    public void mail(Channel channel, String to, String subj, String body, int msgId) {
        executor.execute(() -> {
            try {
                mailWrapper.send(to, subj, body, null);
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(new ResponseMessage(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NOTIFICATION_EXCEPTION);
            }
        });
    }

    public void mail(User user, String to, String subj, String body) {
        executor.execute(() -> {
            try {
                mailWrapper.send(to, subj, body, null);
            } catch (Exception e) {
                log(user.name, e.getMessage());
            }
        });
    }

    public void twit(Channel channel, String token, String secret, String body, int msgId) {
        executor.execute(() -> {
            try {
                twitterWrapper.send(token, secret, body);
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(new ResponseMessage(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NOTIFICATION_EXCEPTION);
            }
        });
    }

    public void push(Channel channel, Notification widget, String body, int dashId, int msgId) {
        if (widget.androidTokens.size() != 0) {
            for (String token : widget.androidTokens.values()) {
                push(channel, new AndroidGCMMessage(token, widget.priority, body, dashId), msgId);
            }
        }

        if (widget.iOSTokens.size() != 0) {
            for (String token : widget.iOSTokens.values()) {
                push(channel, new IOSGCMMessage(token, widget.priority, body, dashId), msgId);
            }
        }
    }

    private void push(Channel channel, GCMMessage message, int msgId) {
        executor.execute(() -> {
            try {
                gcmWrapper.send(message);
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(new ResponseMessage(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NOTIFICATION_EXCEPTION);
            }
        });
    }

    public void push(User user, Notification widget, String body, int dashId) {
        if (widget.androidTokens.size() != 0) {
            for (String token : widget.androidTokens.values()) {
                push(user, new AndroidGCMMessage(token, widget.priority, body, dashId));
            }
        }

        if (widget.iOSTokens.size() != 0) {
            for (String token : widget.iOSTokens.values()) {
                push(user, new IOSGCMMessage(token, widget.priority, body, dashId));
            }
        }
    }

    private void push(User user, GCMMessage message) {
        executor.execute(() -> {
            try {
                gcmWrapper.send(message);
            } catch (Exception e) {
                log(user.name, e.getMessage());
            }
        });
    }

    public void stop() {
        executor.shutdown();
    }

    private void log(Channel channel, String errorMessage, int msgId, int response) {
        User user = getStateUser(channel);
        if (user != null) {
            log(user.name, errorMessage);

            channel.eventLoop().execute(() -> {
                channel.writeAndFlush(new ResponseMessage(msgId, response));
            });
        }
    }

    private void log(String username, String errorMessage) {
        ThreadContext.put("user", username);
        log.error("Error performing blocking IO. {}", errorMessage);
        ThreadContext.clearMap();
    }

}
