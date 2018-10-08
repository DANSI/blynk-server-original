package cc.blynk.server.core.protocol.handlers.encoders;

import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Just wraps ByteBuf into WebSockets frame.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 15.01.16.
 */
@ChannelHandler.Sharable
public class WSMessageEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(WSMessageEncoder.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.debug("In webapp socket encoder {}", msg);
        if (msg instanceof MessageBase) {
            MessageBase message = (MessageBase) msg;
            ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
            out.writeByte(message.command);
            out.writeShort(message.id);

            if (message instanceof ResponseMessage) {
                out.writeInt(((ResponseMessage) message).code);
            } else {
                byte[] body = message.getBytes();
                if (body.length > 0) {
                    out.writeBytes(body);
                }
            }
            super.write(ctx, new BinaryWebSocketFrame(out), promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
