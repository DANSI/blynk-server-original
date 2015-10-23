package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.ReportingDao;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.handlers.hardware.logic.*;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
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

    public HardwareHandler(ServerProperties props, SessionDao sessionDao, ReportingDao reportingDao,
                           BlockingIOProcessor blockingIOProcessor, HandlerState handlerState) {
        super(props, handlerState);
        this.hardware = new HardwareLogic(sessionDao, reportingDao);
        this.bridge = new BridgeLogic(sessionDao);

        long defaultNotificationQuotaLimit = props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
        this.email = new MailLogic(blockingIOProcessor, defaultNotificationQuotaLimit);
        this.push = new PushLogic(blockingIOProcessor, defaultNotificationQuotaLimit);
        this.tweet = new TweetLogic(blockingIOProcessor, defaultNotificationQuotaLimit);
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
                email.messageReceived(ctx, state, msg);
                break;
            case PUSH_NOTIFICATION :
                push.messageReceived(ctx, state, msg);
                break;
            case TWEET :
                tweet.messageReceived(ctx, state, msg);
                break;
        }
    }

}
