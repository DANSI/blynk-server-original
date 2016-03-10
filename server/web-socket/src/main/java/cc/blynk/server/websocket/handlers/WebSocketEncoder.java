package cc.blynk.server.websocket.handlers;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 15.01.16.
 */
public class WebSocketEncoder extends MessageToMessageEncoder<MessageBase> {

    private final GlobalStats stats;

    public WebSocketEncoder(GlobalStats stats) {
        this.stats = stats;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageBase msg, List<Object> out) throws Exception {
        stats.mark(msg.command);

        ByteBuf bb;
        if (msg.command == Command.RESPONSE) {
            bb = ctx.alloc().directBuffer(5);
        } else {
            bb = ctx.alloc().directBuffer(5 + msg.length);
        }
        bb.writeByte(msg.command);
        bb.writeShort(msg.id);
        bb.writeShort(msg.length);
        final byte[] data = msg.getBytes();
        if (data != null) {
            bb.writeBytes(data);
        }

        out.add(new BinaryWebSocketFrame(bb));
    }
}
