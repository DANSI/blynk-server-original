package cc.blynk.server.handlers.common;

import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class PingLogic {

    public static void messageReceived(ChannelHandlerContext ctx, int messageId) {
        ctx.writeAndFlush(new ResponseMessage(messageId, OK), ctx.voidPromise());
    }

}
