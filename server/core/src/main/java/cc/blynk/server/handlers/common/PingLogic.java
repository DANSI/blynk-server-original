package cc.blynk.server.handlers.common;

import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.internal.BlynkByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class PingLogic {

    private PingLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, int messageId) {
        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(ok(messageId), ctx.voidPromise());
        }
    }

}
