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
        // Will use the first 5 bytes to detect a protocol.
        if (in.readableBytes() < 5) {
            return;
        }

        //we can't simply read 5 bytes as 1 number, so split on 4 bytes and 1 byte read
        long header4Bytes = in.getUnsignedInt(0);
        short lastByteOfHeader = in.getUnsignedByte(4);

        ChannelPipeline pipeline = ctx.pipeline();
        buildPipeline(pipeline, header4Bytes, lastByteOfHeader).remove(this);
    }

    private ChannelPipeline buildPipeline(ChannelPipeline pipeline, long header4Bytes, short lastByteOfHeader) {
        if (isHttp(header4Bytes)) {
            return buildHttpPipeline(pipeline);
        }
        if (isHardwarePipeline(header4Bytes, lastByteOfHeader)) {
            return buildHardwarePipeline(pipeline);
        }
        return buildAppPipeline(pipeline);
    }

    private static boolean isHardwarePipeline(long header4Bytes, short lastByteOfHeader) {
        return lastByteOfHeader == 32 && (header4Bytes == 486539520L || header4Bytes == 33554688L);
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

    public abstract ChannelPipeline buildAppPipeline(ChannelPipeline pipeline);

    public abstract ChannelPipeline buildHardwarePipeline(ChannelPipeline pipeline);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleUnexpectedException(ctx, cause);
    }
}
