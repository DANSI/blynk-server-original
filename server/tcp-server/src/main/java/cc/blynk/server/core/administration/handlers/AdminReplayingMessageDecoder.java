package cc.blynk.server.core.administration.handlers;

import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.utils.Config;
import cc.blynk.server.core.administration.model.AdminMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AdminReplayingMessageDecoder extends ReplayingDecoder<Void> implements DefaultExceptionHandler {

    protected static final Logger log = LogManager.getLogger(AdminReplayingMessageDecoder.class);

    private static String[] readParams(ByteBuf in, short paramsNumber) {
        String[] params = new String[paramsNumber];
        for (int i = 0; i < paramsNumber; i++) {
            int paramLength = in.readUnsignedShort();
            params[i] = in.readSlice(paramLength).toString(Config.DEFAULT_CHARSET);
        }
        return params;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        short paramsNumber = in.readUnsignedByte();
        String[] params = readParams(in , paramsNumber);

        int length = in.readUnsignedShort();
        byte[] classData = new byte[length];
        in.readBytes(classData);
        out.add(new AdminMessage(classData, params));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
