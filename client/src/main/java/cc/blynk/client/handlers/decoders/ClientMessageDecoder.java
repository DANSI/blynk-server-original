package cc.blynk.client.handlers.decoders;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetEnhancedGraphDataBinaryMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetGraphDataBinaryMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetProjectByCloneCodeBinaryMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetProjectByTokenBinaryMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoadProfileGzippedBinaryMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;

/**
 * Decodes input byte array into java message.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class ClientMessageDecoder extends ByteToMessageDecoder implements DefaultExceptionHandler {

    protected static final Logger log = LogManager.getLogger(ClientMessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 5) {
            return;
        }

        in.markReaderIndex();

        short command = in.readUnsignedByte();
        int messageId = in.readUnsignedShort();

        MessageBase message;
        if (command == Command.RESPONSE) {
            int responseCode = in.readUnsignedShort();
            message = new ResponseMessage(messageId, responseCode);
        } else {
            int length = in.readUnsignedShort();

            if (in.readableBytes() < length) {
                in.resetReaderIndex();
                return;
            }

            ByteBuf buf = in.readSlice(length);
            switch (command) {
                case Command.GET_GRAPH_DATA_RESPONSE :
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    message = new GetGraphDataBinaryMessage(messageId, bytes);
                    break;
                case Command.GET_ENHANCED_GRAPH_DATA :
                    bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    message = new GetEnhancedGraphDataBinaryMessage(messageId, bytes);
                    break;
                case Command.LOAD_PROFILE_GZIPPED :
                    bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    message = new LoadProfileGzippedBinaryMessage(messageId, bytes);
                    break;
                case Command.GET_PROJECT_BY_TOKEN :
                    bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    message = new GetProjectByTokenBinaryMessage(messageId, bytes);
                    break;
                case Command.GET_PROJECT_BY_CLONE_CODE :
                    bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    message = new GetProjectByCloneCodeBinaryMessage(messageId, bytes);
                    break;

                default:
                    message = produce(messageId, command, buf.toString(StandardCharsets.UTF_8));
            }

        }

        log.trace("Incoming client {}", message);

        out.add(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
