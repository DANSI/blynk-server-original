package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.handlers.hardware.logic.*;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
@ChannelHandler.Sharable
public class HardwareHandler extends BaseSimpleChannelInboundHandler<Message> {

    private final HardwareLogic hardware;
    private final MailLogic email;
    private final BridgeLogic bridge;
    private final PushLogic push;
    private final TweetLogic tweet;

    public HardwareHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder, StorageDao storageDao,
                           NotificationsProcessor notificationsProcessor) {
        super(props, userRegistry, sessionsHolder);
        this.hardware = new HardwareLogic(props, sessionsHolder, storageDao);
        this.bridge = new BridgeLogic(sessionsHolder);

        long defaultNotificationQuotaLimit = props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
        this.email = new MailLogic(notificationsProcessor, defaultNotificationQuotaLimit);
        this.push = new PushLogic(notificationsProcessor, defaultNotificationQuotaLimit);
        this.tweet = new TweetLogic(notificationsProcessor, defaultNotificationQuotaLimit);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, Message msg) {
        switch (msg.command) {
            case HARDWARE:
                hardware.messageReceived(ctx, user, msg);
                break;
            case PING :
                PingLogic.messageReceived(ctx, msg.id);
                break;
            case EMAIL :
                email.messageReceived(ctx, user, msg);
                break;
            case BRIDGE :
                bridge.messageReceived(ctx, user, msg);
                break;
            case PUSH_NOTIFICATION :
                push.messageReceived(ctx, user,msg);
                break;
            case TWEET :
                tweet.messageReceived(ctx, user, msg);
                break;
        }
    }

    @Override
    public void updateProperties(ServerProperties props) {
        super.updateProperties(props);
        hardware.updateProperties(props);
    }
}
