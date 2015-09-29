package cc.blynk.server.handlers;

import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.exceptions.QuotaLimitException;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.stats.metrics.InstanceLoadMeter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;
import org.apache.logging.log4j.ThreadContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public abstract class BaseSimpleChannelInboundHandler<I extends MessageBase> extends ChannelInboundHandlerAdapter implements DefaultExceptionHandler {

    protected final int USER_QUOTA_LIMIT_WARN_PERIOD;
    protected final int USER_QUOTA_LIMIT;
    private final TypeParameterMatcher matcher;
    private final HandlerState handlerState;
    private final InstanceLoadMeter quotaMeter;
    private long lastQuotaExceededTime;

    protected BaseSimpleChannelInboundHandler(ServerProperties props, HandlerState handlerState) {
        this.matcher = TypeParameterMatcher.find(this, BaseSimpleChannelInboundHandler.class, "I");
        this.handlerState = handlerState;
        this.USER_QUOTA_LIMIT = props.getIntProperty("user.message.quota.limit");
        this.USER_QUOTA_LIMIT_WARN_PERIOD = props.getIntProperty("user.message.quota.limit.exceeded.warning.period");
        this.quotaMeter = new InstanceLoadMeter();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (matcher.match(msg)) {
            User user = handlerState.user;
            I typedMsg = (I) msg;

            try {
                ThreadContext.put("user", user.name);
                if (quotaMeter.getOneMinuteRate() > USER_QUOTA_LIMIT) {
                    sendErrorResponseIfTicked(ctx, typedMsg.id);
                    return;
                }

                quotaMeter.mark();
                messageReceived(ctx, handlerState, typedMsg);
            } catch (Exception e) {
                handleGeneralException(ctx, e);
            } finally {
                ThreadContext.clearMap();
                ReferenceCountUtil.release(msg);
            }
        }
    }

    private void sendErrorResponseIfTicked(ChannelHandlerContext ctx, int msgId) {
        long now = System.currentTimeMillis();
        //once a minute sending user response message in case limit is exceeded constantly
        if (lastQuotaExceededTime + USER_QUOTA_LIMIT_WARN_PERIOD < now) {
            lastQuotaExceededTime = now;
            throw new QuotaLimitException("User has exceeded message quota limit.", msgId);
        }
    }

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     *
     * Is called for each message of type {@link I}.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     */
    protected abstract void messageReceived(ChannelHandlerContext ctx, HandlerState state, I msg);

    public HandlerState getHandlerState() {
        return handlerState;
    }

    public InstanceLoadMeter getQuotaMeter() {
        return quotaMeter;
    }
}
