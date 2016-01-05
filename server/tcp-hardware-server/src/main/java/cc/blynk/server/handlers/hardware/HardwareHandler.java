package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.PingLogic;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.handlers.hardware.logic.*;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.ThreadContext;

import static cc.blynk.common.enums.Command.*;

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
    private final TweetLogic tweet;
    private final HardwareSyncLogic sync;
    private final HardwareInfoLogic info;

    public HardwareHandler(ServerProperties props, SessionDao sessionDao, ReportingDao reportingDao,
                           BlockingIOProcessor blockingIOProcessor, HardwareStateHolder stateHolder) {
        super(props, stateHolder);
        this.hardware = new HardwareLogic(sessionDao, reportingDao);
        this.bridge = new BridgeLogic(sessionDao);

        long defaultNotificationQuotaLimit = props.getLongProperty("notifications.frequency.user.quota.limit") * 1000;
        this.email = new MailLogic(blockingIOProcessor, defaultNotificationQuotaLimit);
        this.push = new PushLogic(blockingIOProcessor, defaultNotificationQuotaLimit);
        this.tweet = new TweetLogic(blockingIOProcessor, defaultNotificationQuotaLimit);
        this.sync = new HardwareSyncLogic();
        this.info = new HardwareInfoLogic(props);

        this.state = stateHolder;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, StringMessage msg) {
        ThreadContext.put("user", state.user.name);
        switch (msg.command) {
            case HARDWARE:
                hardware.messageReceived(state, msg);
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


    //for test only
    public HardwareStateHolder getState() {
        return state;
    }

}
