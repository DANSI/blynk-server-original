package cc.blynk.server.api.http.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleUnexpectedException;

/**
 * Base handler that detects protocol between http and blynk app protocol.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 15.02.18.
 */
public abstract class BaseHttpAndBlynkUnificationHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
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
        if (isHttp(magic)) {
            return buildHttpPipeline(pipeline);
        }
        return buildBlynkPipeline(pipeline);
    }

    /**
     * See HttpSignatureTest for more details
     */
    private static boolean isHttp(long httpHeader4Bytes) {
        return
                httpHeader4Bytes == 1195725856L || // 'GET '
                httpHeader4Bytes == 1347375956L || // 'POST'
                httpHeader4Bytes == 1347769376L || // 'PUT '
                httpHeader4Bytes == 1212498244L || // 'HEAD'
                httpHeader4Bytes == 1330664521L || // 'OPTI'
                httpHeader4Bytes == 1346458691L || // 'PATC'
                httpHeader4Bytes == 1145392197L || // 'DELE'
                httpHeader4Bytes == 1414676803L || // 'TRAC'
                httpHeader4Bytes == 1129270862L;   // 'CONN'
    }

    public abstract ChannelPipeline buildHttpPipeline(ChannelPipeline pipeline);

    public abstract ChannelPipeline buildBlynkPipeline(ChannelPipeline pipeline);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleUnexpectedException(ctx, cause);
    }
}
