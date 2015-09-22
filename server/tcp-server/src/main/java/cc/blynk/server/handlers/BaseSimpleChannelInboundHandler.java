package cc.blynk.server.handlers;

import cc.blynk.common.exceptions.BaseServerException;
import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.auth.User;
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

    private final TypeParameterMatcher matcher;
    private final HandlerState handlerState;

    protected BaseSimpleChannelInboundHandler(ServerProperties props, HandlerState handlerState) {
        this.matcher = TypeParameterMatcher.find(this, BaseSimpleChannelInboundHandler.class, "I");
        this.handlerState = handlerState;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (matcher.match(msg)) {
            User user = handlerState.user;
            I typedMsg = (I) msg;

            try {
                ThreadContext.put("user", user.name);
                messageReceived(ctx, handlerState, typedMsg);
                ThreadContext.clearMap();
            } catch (BaseServerException cause) {
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
    protected abstract void messageReceived(ChannelHandlerContext ctx, HandlerState state, I msg);

    public HandlerState getHandlerState() {
        return handlerState;
    }
}
