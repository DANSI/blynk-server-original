package cc.blynk.server.common.handlers;

import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleGeneralException;
import static cc.blynk.server.internal.CommonByteBufUtil.alreadyRegistered;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
@ChannelHandler.Sharable
public class AlreadyLoggedHandler extends SimpleChannelInboundHandler<MessageBase> {

    private static final Logger log = LogManager.getLogger(AlreadyLoggedHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageBase msg) {
        if (msg instanceof LoginMessage) {
            if (ctx.channel().isWritable()) {
                ctx.writeAndFlush(alreadyRegistered(msg.id), ctx.voidPromise());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Hardware not logged. {}. Closing.", ctx.channel().remoteAddress());
            }
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleGeneralException(ctx, cause);
    }

}
