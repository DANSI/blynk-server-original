package cc.blynk.server.handlers;

import cc.blynk.common.exceptions.BaseServerException;
import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;
import org.apache.logging.log4j.ThreadContext;

import static cc.blynk.common.enums.Response.QUOTA_LIMIT_EXCEPTION;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public abstract class BaseSimpleChannelInboundHandler<I extends MessageBase> extends ChannelInboundHandlerAdapter implements DefaultExceptionHandler {

    protected final UserRegistry userRegistry;
    protected final SessionsHolder sessionsHolder;
    private final TypeParameterMatcher matcher;
    private volatile int USER_QUOTA_LIMIT;
    private volatile int USER_QUOTA_LIMIT_WARN_PERIOD;

    protected BaseSimpleChannelInboundHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        this.userRegistry = userRegistry;
        this.sessionsHolder = sessionsHolder;
        this.matcher = TypeParameterMatcher.find(this, BaseSimpleChannelInboundHandler.class, "I");
        updateProperties(props);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (matcher.match(msg)) {
            User user = null;
            try {
                I imsg = (I) msg;
                user = ctx.channel().attr(ChannelState.USER).get();
                if (user == null) {
                    log.error("User not logged. {}. Closing.", ctx.channel().remoteAddress());
                    ctx.close();
                    return;
                }

                if (user.getQuotaMeter().getOneMinuteRate() > USER_QUOTA_LIMIT) {
                    long now = System.currentTimeMillis();
                    //once a minute sending user response message in case limit is exceeded constantly
                    if (user.getLastQuotaExceededTime() + USER_QUOTA_LIMIT_WARN_PERIOD < now) {
                        user.setLastQuotaExceededTime(now);
                        log.warn("User '{}' had exceeded {} rec/sec limit. Ip : {}", user.getName(), USER_QUOTA_LIMIT, ctx.channel().remoteAddress());
                        ctx.writeAndFlush(produce(imsg.id, QUOTA_LIMIT_EXCEPTION));
                    }
                    return;
                }
                user.incrStat(imsg.command);

                ThreadContext.put("user", user.getName());
                messageReceived(ctx, user, imsg);
                ThreadContext.clearMap();
            } catch (BaseServerException cause) {
                if (user != null) {
                    user.incrException(cause.errorCode);
                }
                handleAppException(ctx, cause);
            } catch (Exception e) {
                handleUnexpectedException(ctx, e);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
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
    protected abstract void messageReceived(ChannelHandlerContext ctx, User user, I msg);

    /**
     *  When property file changed during server work, to avoid restart,
     *  so every child overrides it's property.
     */
    public void updateProperties(ServerProperties props) {
        try {
            this.USER_QUOTA_LIMIT = props.getIntProperty("user.message.quota.limit");
        } catch (RuntimeException e) {
            //error already logged, so do nothing.
        }
        try {
            this.USER_QUOTA_LIMIT_WARN_PERIOD = props.getIntProperty("user.message.quota.limit.exceeded.warning.period");
        } catch (RuntimeException e) {
            //error already logged, so do nothing.
        }
    }

}
