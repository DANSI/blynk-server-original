package cc.blynk.client.handlers.decoders;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.handlers.decoders.MobileMessageDecoder;
import cc.blynk.server.core.protocol.model.messages.BinaryMessage;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleGeneralException;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;

/**
 * Decodes input byte array into java message.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AppClientMessageDecoder extends ByteToMessageDecoder {

    protected static final Logger log = LogManager.getLogger(AppClientMessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < MobileMessageDecoder.PROTOCOL_APP_HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();

        short command = in.readUnsignedByte();
        int messageId = in.readUnsignedShort();

        MessageBase message;
        if (command == Command.RESPONSE) {
            int responseCode = (int) in.readUnsignedInt();
            message = new ResponseMessage(messageId, responseCode);
        } else {
            int length = (int) in.readUnsignedInt();

            if (in.readableBytes() < length) {
                in.resetReaderIndex();
                return;
            }

            ByteBuf buf = in.readSlice(length);
            switch (command) {
                case GET_ENHANCED_GRAPH_DATA :
                case GET_PROJECT_BY_CLONE_CODE :
                case LOAD_PROFILE_GZIPPED :
                case GET_PROJECT_BY_TOKEN :
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    message = new BinaryMessage(messageId, command, bytes);
                    break;
                default:
                    message = produce(messageId, command, buf.toString(StandardCharsets.UTF_8));
            }

        }

        log.trace("Incoming client {}", message);

        out.add(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleGeneralException(ctx, cause);
    }
}
