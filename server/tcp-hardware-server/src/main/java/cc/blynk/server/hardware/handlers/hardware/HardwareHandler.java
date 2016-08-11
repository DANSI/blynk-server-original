package cc.blynk.server.hardware.handlers.hardware;

import cc.blynk.server.Holder;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.BridgeLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.HardwareInfoLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.HardwareLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.HardwareSyncLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.MailLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.PushLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.SetWidgetPropertyLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.SmsLogic;
import cc.blynk.server.hardware.handlers.hardware.logic.TwitLogic;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.ThreadContext;

import static cc.blynk.server.core.protocol.enums.Command.BRIDGE;
import static cc.blynk.server.core.protocol.enums.Command.EMAIL;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_INFO;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.PING;
import static cc.blynk.server.core.protocol.enums.Command.PUSH_NOTIFICATION;
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;
import static cc.blynk.server.core.protocol.enums.Command.SMS;
import static cc.blynk.server.core.protocol.enums.Command.TWEET;

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
    private final SmsLogic smsLogic;
    private final SetWidgetPropertyLogic propertyLogic;
    private final HardwareSyncLogic sync;
    private final HardwareInfoLogic info;

    public HardwareHandler(Holder holder, HardwareStateHolder stateHolder) {
        super(holder.props, stateHolder);
        this.hardware = new HardwareLogic(holder.sessionDao, holder.reportingDao);
        this.bridge = new BridgeLogic(holder.sessionDao, hardware);

        final long defaultNotificationQuotaLimit = holder.props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
        this.email = new MailLogic(holder.blockingIOProcessor, holder.mailWrapper, defaultNotificationQuotaLimit);
        this.push = new PushLogic(holder.blockingIOProcessor, holder.gcmWrapper, defaultNotificationQuotaLimit);
        this.tweet = new TwitLogic(holder.blockingIOProcessor, holder.twitterWrapper, defaultNotificationQuotaLimit);
        this.smsLogic = new SmsLogic(holder.smsWrapper, defaultNotificationQuotaLimit);
        this.propertyLogic = new SetWidgetPropertyLogic(holder.sessionDao);
        this.sync = new HardwareSyncLogic();
        this.info = new HardwareInfoLogic(holder.props.getIntProperty("hard.socket.idle.timeout", 0));

        this.state = stateHolder;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
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
            case SMS :
                smsLogic.messageReceived(ctx, state, msg);
                break;
            case HARDWARE_SYNC :
                sync.messageReceived(ctx, state, msg);
                break;
            case HARDWARE_INFO :
                info.messageReceived(ctx, state, msg);
                break;
            case SET_WIDGET_PROPERTY :
                propertyLogic.messageReceived(ctx, state, msg);
                break;

        }
    }

}
