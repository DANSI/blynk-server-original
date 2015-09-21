package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.exceptions.QuotaLimitException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.handlers.hardware.logic.*;
import cc.blynk.server.stats.metrics.InstanceLoadMeter;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
public class HardwareHandler extends BaseSimpleChannelInboundHandler<Message> {

    private final int USER_QUOTA_LIMIT;
    private final int USER_QUOTA_LIMIT_WARN_PERIOD;

    private final HardwareLogic hardware;
    private final MailLogic email;
    private final BridgeLogic bridge;
    private final PushLogic push;
    private final TweetLogic tweet;

    private InstanceLoadMeter quotaMeter;
    private long lastQuotaExceededTime;

    public HardwareHandler(ServerProperties props, SessionsHolder sessionsHolder, StorageDao storageDao,
                           NotificationsProcessor notificationsProcessor, HandlerState handlerState) {
        super(props, handlerState);
        this.hardware = new HardwareLogic(sessionsHolder, storageDao);
        this.bridge = new BridgeLogic(sessionsHolder);

        long defaultNotificationQuotaLimit = props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
        this.email = new MailLogic(notificationsProcessor, defaultNotificationQuotaLimit);
        this.push = new PushLogic(notificationsProcessor, defaultNotificationQuotaLimit);
        this.tweet = new TweetLogic(notificationsProcessor, defaultNotificationQuotaLimit);

        this.USER_QUOTA_LIMIT = props.getIntProperty("user.message.quota.limit");
        this.USER_QUOTA_LIMIT_WARN_PERIOD = props.getIntProperty("user.message.quota.limit.exceeded.warning.period");
        this.quotaMeter = new InstanceLoadMeter();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message msg) {
        if (quotaMeter.getOneMinuteRate() > USER_QUOTA_LIMIT) {
            sendErrorResponseIfTicked(msg.id);
            return;
        }
        quotaMeter.mark();

        switch (msg.command) {
            case HARDWARE:
                hardware.messageReceived(ctx, state, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            case BRIDGE :
                bridge.messageReceived(ctx, state, msg);
                break;
            case EMAIL :
                email.messageReceived(ctx, state.user, msg);
                break;
            case PUSH_NOTIFICATION :
                push.messageReceived(ctx, state.user,msg);
                break;
            case TWEET :
                tweet.messageReceived(ctx, state.user, msg);
                break;
        }
    }

    private void sendErrorResponseIfTicked(int msgId) {
        long now = System.currentTimeMillis();
        //once a minute sending user response message in case limit is exceeded constantly
        if (lastQuotaExceededTime + USER_QUOTA_LIMIT_WARN_PERIOD < now) {
            lastQuotaExceededTime = now;
            throw new QuotaLimitException("User has exceeded message quota limit.", msgId);
        }
    }

}
