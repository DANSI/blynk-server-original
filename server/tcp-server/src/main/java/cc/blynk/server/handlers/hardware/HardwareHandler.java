package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.handlers.hardware.logic.*;
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

    private final HardwareLogic hardware;
    private final MailLogic email;
    private final BridgeLogic bridge;
    private final PushLogic push;
    private final TweetLogic tweet;

    public HardwareHandler(ServerProperties props, SessionsHolder sessionsHolder, StorageDao storageDao,
                           NotificationsProcessor notificationsProcessor, HandlerState handlerState) {
        super(props, handlerState);
        this.hardware = new HardwareLogic(sessionsHolder, storageDao);
        this.bridge = new BridgeLogic(sessionsHolder);

        long defaultNotificationQuotaLimit = props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
        this.email = new MailLogic(notificationsProcessor, defaultNotificationQuotaLimit);
        this.push = new PushLogic(notificationsProcessor, defaultNotificationQuotaLimit);
        this.tweet = new TweetLogic(notificationsProcessor, defaultNotificationQuotaLimit);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message msg) {
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

}
