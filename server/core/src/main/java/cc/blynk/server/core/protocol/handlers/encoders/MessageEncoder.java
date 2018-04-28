package cc.blynk.server.core.protocol.handlers.encoders;

import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encodes java message into a bytes array.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class MessageEncoder extends MessageToByteEncoder<MessageBase> {

    private final GlobalStats stats;

    public MessageEncoder(GlobalStats stats) {
        this.stats = stats;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageBase message, ByteBuf out) {
        out.writeByte(message.command);
        out.writeShort(message.id);

        if (message instanceof ResponseMessage) {
            out.writeShort(((ResponseMessage) message).code);
        } else {
            stats.mark(message.command);

            byte[] body = message.getBytes();
            out.writeShort(body.length);
            if (body.length > 0) {
                out.writeBytes(body);
            }
        }
    }
}
