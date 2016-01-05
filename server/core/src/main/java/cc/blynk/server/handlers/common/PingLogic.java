package cc.blynk.server.handlers.common;

import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class PingLogic {

    public static void messageReceived(ChannelHandlerContext ctx, int messageId) {
        ctx.writeAndFlush(produce(messageId, OK));
    }

}
