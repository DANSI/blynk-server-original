package cc.blynk.common.handlers.common.encoders;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.common.utils.Config;
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

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageBase message, ByteBuf out) throws Exception {
        out.writeByte(message.command);
        out.writeShort(message.id);
        out.writeShort(message.length);

        if (message.command != Command.RESPONSE && message.length > 0) {
            out.writeBytes(((Message) message).body.getBytes(Config.DEFAULT_CHARSET));
        }
    }
}
