package cc.blynk.server.workers.notifications;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.protocol.appllication.GetGraphDataResponseMessage;
import cc.blynk.common.utils.Config;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.handlers.app.logic.reporting.GraphPinRequest;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Notification;
import cc.blynk.server.notifications.AndroidGCMMessage;
import cc.blynk.server.notifications.GCMMessage;
import cc.blynk.server.notifications.GCMWrapper;
import cc.blynk.server.notifications.IOSGCMMessage;
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
import static cc.blynk.server.utils.ByteUtils.compress;
import static cc.blynk.server.utils.HandlerUtil.getState;

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
        this.gcmWrapper = new GCMWrapper();
        this.reportingDao = reportingDao;
        this.executor = new ThreadPoolExecutor(
                NOTIFICATIONS_PROCESSORS, NOTIFICATIONS_PROCESSORS,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
        this.tokenBody = tokenBody;
    }

    public void readGraphData(Channel channel, String name, GraphPinRequest[] requestedPins, int msgId) {
        executor.execute(() -> {
            try {

                byte[][] data = reportingDao.getAllFromDisk(name, requestedPins, msgId);
                byte[] compressed = compress(data, msgId);

                log.trace("Sending getGraph response. ");
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(new GetGraphDataResponseMessage(msgId, compressed));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NO_DATA_EXCEPTION);
            }
        });
    }

    public void readGraphDataNewAPI(Channel channel, String name, GraphPinRequest[] requestedPins, int msgId) {
        executor.execute(() -> {
            try {
                byte[][] data = reportingDao.getAllFromDisk(name, requestedPins, msgId);
                byte[] compressed = compress(requestedPins[0].dashId, data, msgId);

                log.trace("Sending getGraph response. ");
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(new GetGraphDataResponseMessage(msgId, compressed));
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
                    channel.writeAndFlush(produce(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NOTIFICATION_EXCEPTION);
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
                log(channel, e.getMessage(), msgId, Response.NOTIFICATION_EXCEPTION);
            }
        });
    }

    public void push(Channel channel, Notification widget, String body, int msgId) {
        if (widget.token != null && !widget.token.equals("")) {
            push(channel, new AndroidGCMMessage(widget.token, body), msgId);
        }
        if (widget.iOSToken != null && !widget.iOSToken.equals("")) {
            push(channel, new IOSGCMMessage(widget.iOSToken, body), msgId);
        }
    }

    private void push(Channel channel, GCMMessage message, int msgId) {
        executor.execute(() -> {
            try {
                gcmWrapper.send(message);
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(produce(msgId, OK));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NOTIFICATION_EXCEPTION);
            }
        });
    }

    public void push(User user, Notification widget, String body) {
        if (widget.token != null && !widget.token.equals("")) {
            push(user, new AndroidGCMMessage(widget.token, body));
        }
        if (widget.iOSToken != null && !widget.iOSToken.equals("")) {
            push(user, new IOSGCMMessage(widget.iOSToken, body));
        }
    }

    private void push(User user, GCMMessage message) {
        executor.execute(() -> {
            try {
                gcmWrapper.send(message);
            } catch (Exception e) {
                log(user, e.getMessage());
            }
        });
    }

    public void stop() {
        executor.shutdown();
    }

    private void log(Channel channel, String errorMessage, int msgId, int response) {
        User user = getState(channel).user;

        log(user, errorMessage);

        channel.eventLoop().execute(() -> {
            channel.writeAndFlush(new ResponseMessage(msgId, Command.RESPONSE, response));
        });
    }

    private void log(User user, String errorMessage) {
        if (user != null) {
            ThreadContext.put("user", user.name);
            log.error("Error performing blocking IO. {}", errorMessage);
            ThreadContext.clearMap();
        }
    }

}
