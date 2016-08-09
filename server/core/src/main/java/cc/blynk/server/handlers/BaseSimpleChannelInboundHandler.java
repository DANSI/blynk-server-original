package cc.blynk.server.handlers;

import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.session.StateHolder;
import cc.blynk.server.core.stats.metrics.InstanceLoadMeter;
import cc.blynk.utils.ServerProperties;
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
public abstract class BaseSimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter implements DefaultExceptionHandler {

    public final StateHolder state;
    protected final int USER_QUOTA_LIMIT_WARN_PERIOD;
    protected final int USER_QUOTA_LIMIT;
    private final TypeParameterMatcher matcher;
    private final InstanceLoadMeter quotaMeter;
    private long lastQuotaExceededTime;

    protected BaseSimpleChannelInboundHandler(ServerProperties props, StateHolder state) {
        this.matcher = TypeParameterMatcher.find(this, BaseSimpleChannelInboundHandler.class, "I");
        this.USER_QUOTA_LIMIT = props.getIntProperty("user.message.quota.limit");
        this.USER_QUOTA_LIMIT_WARN_PERIOD = props.getIntProperty("user.message.quota.limit.exceeded.warning.period");
        this.quotaMeter = new InstanceLoadMeter();
        this.state = state;
    }

    private static int getMsgId(Object o) {
        if (o instanceof MessageBase) {
            return ((MessageBase) o).id;
        }
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (matcher.match(msg)) {
            final I typedMsg = (I) msg;
            try {
                if (quotaMeter.getOneMinuteRate() > USER_QUOTA_LIMIT) {
                    sendErrorResponseIfTicked();
                    return;
                }
                quotaMeter.mark();
                messageReceived(ctx, typedMsg);
            } catch (Exception e) {
                handleGeneralException(ctx, e, getMsgId(msg));
            } finally {
                ThreadContext.clearMap();
                ReferenceCountUtil.release(msg);
            }
        }
    }

    private void sendErrorResponseIfTicked() {
        long now = System.currentTimeMillis();
        //once a minute sending user response message in case limit is exceeded constantly
        if (lastQuotaExceededTime + USER_QUOTA_LIMIT_WARN_PERIOD < now) {
            lastQuotaExceededTime = now;
            throw new QuotaLimitException("User has exceeded message quota limit.");
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
    public abstract void messageReceived(ChannelHandlerContext ctx, I msg);

    public InstanceLoadMeter getQuotaMeter() {
        return quotaMeter;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
