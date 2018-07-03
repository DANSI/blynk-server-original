package cc.blynk.server.common.handlers;

import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.appllication.RegisterMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleGeneralException;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
@ChannelHandler.Sharable
public class UserNotLoggedHandler extends SimpleChannelInboundHandler<MessageBase> {

    private static final Logger log = LogManager.getLogger(Logger.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageBase msg) {
        log.debug("User not logged. {}. Closing.", ctx.channel().remoteAddress());
        if (msg instanceof RegisterMessage) {
            if (ctx.channel().isWritable()) {
                ctx.writeAndFlush(notAllowed(msg.id), ctx.voidPromise());
            }
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleGeneralException(ctx, cause);
    }

}
