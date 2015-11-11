package cc.blynk.server.handlers.common;

import cc.blynk.common.model.messages.MessageBase;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class UserNotLoggedHandler extends SimpleChannelInboundHandler<MessageBase> {

    private static final Logger log = LogManager.getLogger(Logger.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageBase msg) throws Exception {
        //remove itself to not spam logs in case many messages are came.
        //ctx.pipeline().remove(this);
        //this causes another bug.
        //todo fix above code.
        log.error("User not logged. {}. Closing.", ctx.channel().remoteAddress());
        ctx.close();
    }
}
