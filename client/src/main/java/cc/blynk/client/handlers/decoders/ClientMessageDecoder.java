package cc.blynk.client.handlers.decoders;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;
import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.common.model.messages.ResponseWithBodyMessage;
import cc.blynk.common.model.messages.protocol.appllication.GetGraphDataResponseMessage;
import cc.blynk.common.model.messages.protocol.appllication.LoadProfileGzippedBinaryMessage;
import cc.blynk.common.utils.Config;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static cc.blynk.common.model.messages.MessageFactory.produce;

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
            if (responseCode == Response.DEVICE_WENT_OFFLINE_2) {
                message = new ResponseWithBodyMessage(messageId, Command.RESPONSE, responseCode, in.readInt());
            } else {
                message = produce(messageId, responseCode);
            }
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
                    message = new GetGraphDataResponseMessage(messageId, bytes);
                    break;
                case Command.LOAD_PROFILE_GZIPPED :
                    bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    message = new LoadProfileGzippedBinaryMessage(messageId, bytes);
                    break;
                default:
                    message = produce(messageId, command, buf.toString(Config.DEFAULT_CHARSET));
            }

        }

        log.trace("Incoming {}", message);

        out.add(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
