package cc.blynk.server.handlers;

import cc.blynk.server.Limits;
import cc.blynk.server.core.protocol.exceptions.BaseServerException;
import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.session.StateHolderBase;
import cc.blynk.server.core.stats.metrics.InstanceLoadMeter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public abstract class BaseSimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter
        implements DefaultExceptionHandler {

    /*
     * in case of consistent quota limit exceed during long term, sending warning response back to exceeding channel
     * for performance reason sending only 1 message within interval. In millis
     *
     * this property was never changed, so moving it to static field
     */
    private final static int USER_QUOTA_LIMIT_WARN_PERIOD = 60_000;

    private final int userQuotaLimit;
    private final Class<I> type;
    private final InstanceLoadMeter quotaMeter;
    private long lastQuotaExceededTime;
    private static final QuotaLimitException quotaLimitExceptionCached =
            new QuotaLimitException("User has exceeded message quota limit.");

    protected BaseSimpleChannelInboundHandler(Class<I> type, Limits limits) {
        this.type = type;
        this.userQuotaLimit = limits.userQuotaLimit;
        this.quotaMeter = new InstanceLoadMeter();
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
        if (type.isInstance(msg)) {
            try {
                if (quotaMeter.getOneMinuteRate() > userQuotaLimit) {
                    sendErrorResponseIfTicked();
                    return;
                }
                quotaMeter.mark();
                messageReceived(ctx, (I) msg);
            } catch (BaseServerException bse) {
                handleBaseServerException(ctx, bse, getMsgId(msg));
            } catch (Exception e) {
                handleGeneralException(ctx, e);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    private void sendErrorResponseIfTicked() {
        long now = System.currentTimeMillis();
        //once a minute sending user response message in case limit is exceeded constantly
        if (lastQuotaExceededTime + USER_QUOTA_LIMIT_WARN_PERIOD < now) {
            lastQuotaExceededTime = now;
            throw quotaLimitExceptionCached;
        }
    }

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     *
     * Is called for each message of type {@link I}.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this SimpleChannelInboundHandler
     *                      belongs to
     * @param msg           the message to handle
     */
    public abstract void messageReceived(ChannelHandlerContext ctx, I msg);

    public abstract StateHolderBase getState();

    public InstanceLoadMeter getQuotaMeter() {
        return quotaMeter;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
