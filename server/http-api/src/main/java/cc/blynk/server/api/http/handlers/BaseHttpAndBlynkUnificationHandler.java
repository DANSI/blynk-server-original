package cc.blynk.server.api.http.handlers;

import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.utils.HttpUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 15.02.18.
 */
abstract class BaseHttpAndBlynkUnificationHandler extends ByteToMessageDecoder implements DefaultExceptionHandler {

    static final Logger log = LogManager.getLogger(BaseHttpAndBlynkUnificationHandler.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Will use the first 4 bytes to detect a protocol.
        if (in.readableBytes() < 4) {
            return;
        }

        int readerIndex = in.readerIndex();
        long httpHeader4Bytes = in.getUnsignedInt(readerIndex);

        ChannelPipeline pipeline = ctx.pipeline();
        buildPipeline(pipeline, httpHeader4Bytes).remove(this);
    }

    private ChannelPipeline buildPipeline(ChannelPipeline pipeline, long magic) {
        if (HttpUtil.isHttp(magic)) {
            return buildHttpPipeline(pipeline);
        }
        return buildBlynkPipeline(pipeline);
    }

    public abstract ChannelPipeline buildHttpPipeline(ChannelPipeline pipeline);

    public abstract ChannelPipeline buildBlynkPipeline(ChannelPipeline pipeline);

}
