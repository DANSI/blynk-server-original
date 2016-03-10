package cc.blynk.server.handlers.common;

import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class PingLogic {

    public static void messageReceived(ChannelHandlerContext ctx, int messageId) {
        ctx.writeAndFlush(makeResponse(ctx, messageId, OK), ctx.voidPromise());
    }

}
