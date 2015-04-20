package cc.blynk.server.core.administration.handlers;

import cc.blynk.common.handlers.DefaultExceptionHandler;
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

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int length = in.readUnsignedShort();
        byte[] message = in.readSlice(length).array();
        out.add(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
