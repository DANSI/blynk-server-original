package cc.blynk.server.hardware.handlers.hardware;

import cc.blynk.server.Holder;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.*;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.ThreadContext;

import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
public class HardwareHandler extends BaseSimpleChannelInboundHandler<StringMessage> {

    public final HardwareStateHolder state;
    private final HardwareLogic hardware;
    private final MailLogic email;
    private final BridgeLogic bridge;
    private final PushLogic push;
    private final TwitLogic tweet;
    private final HardwareSyncLogic sync;
    private final HardwareInfoLogic info;

    public HardwareHandler(Holder holder, HardwareStateHolder stateHolder) {
        super(holder.props, stateHolder);
        this.hardware = new HardwareLogic(holder.sessionDao, holder.reportingDao);
        this.bridge = new BridgeLogic(holder.sessionDao);

        final long defaultNotificationQuotaLimit = holder.props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
        this.email = new MailLogic(holder.blockingIOProcessor, holder.mailWrapper, defaultNotificationQuotaLimit);
        this.push = new PushLogic(holder.blockingIOProcessor, holder.gcmWrapper, defaultNotificationQuotaLimit);
        this.tweet = new TwitLogic(holder.blockingIOProcessor, holder.twitterWrapper, defaultNotificationQuotaLimit);
        this.sync = new HardwareSyncLogic();
        this.info = new HardwareInfoLogic(holder.props.getIntProperty("hard.socket.idle.timeout", 0));

        this.state = stateHolder;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        ThreadContext.put("user", state.user.name);
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
            case HARDWARE_SYNC :
                sync.messageReceived(ctx, state, msg);
                break;
            case HARDWARE_INFO :
                info.messageReceived(ctx, state, msg);
                break;
        }
    }

}
