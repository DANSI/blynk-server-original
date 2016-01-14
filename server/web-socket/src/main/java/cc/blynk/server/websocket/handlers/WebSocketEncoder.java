package cc.blynk.server.websocket.handlers;

import cc.blynk.server.core.protocol.model.messages.MessageBase;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 15.01.16.
 */
public class WebSocketEncoder extends MessageToMessageEncoder<MessageBase> {


    @Override
    protected void encode(ChannelHandlerContext ctx, MessageBase msg, List<Object> out) throws Exception {
        //todo finish and optimize.
        ByteBuffer bb = ByteBuffer.allocate(5 + msg.length);
        bb.put((byte) msg.command);
        bb.putShort((short) msg.id);
        bb.putShort((short) msg.length);
        byte[] data = msg.getBytes();
        if (data != null) {
            bb.put(data);
        }

        out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bb.array())));
    }
}
