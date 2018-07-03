package cc.blynk.server.common;

import cc.blynk.server.core.protocol.exceptions.BaseServerException;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.session.StateHolderBase;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleBaseServerException;
import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleGeneralException;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public abstract class BaseSimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter {

    protected static final Logger log = LogManager.getLogger(BaseSimpleChannelInboundHandler.class);

    private final Class<I> type;

    protected BaseSimpleChannelInboundHandler(Class<I> type) {
        this.type = type;
    }

    private static int getMsgId(Object o) {
        if (o instanceof MessageBase) {
            return ((MessageBase) o).id;
        }
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (type.isInstance(msg)) {
            try {
                messageReceived(ctx, (I) msg);
            } catch (NumberFormatException nfe) {
                log.debug("Error parsing number. {}", nfe.getMessage());
                ctx.writeAndFlush(illegalCommand(getMsgId(msg)), ctx.voidPromise());
            } catch (BaseServerException bse) {
                handleBaseServerException(ctx, bse, getMsgId(msg));
            } catch (Exception e) {
                handleGeneralException(ctx, e);
            } finally {
                ReferenceCountUtil.release(msg);
            }
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleGeneralException(ctx, cause);
    }
}
