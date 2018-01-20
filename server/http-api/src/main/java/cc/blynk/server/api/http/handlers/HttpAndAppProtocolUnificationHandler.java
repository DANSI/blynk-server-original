package cc.blynk.server.api.http.handlers;

import cc.blynk.core.http.handlers.OTAHandler;
import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.GetServerHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.handlers.decoders.AppMessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.AppMessageEncoder;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.utils.HttpUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 18.01.18.
 */
public class HttpAndAppProtocolUnificationHandler extends ByteToMessageDecoder implements DefaultExceptionHandler {

    private static final Logger log = LogManager.getLogger(HttpAndAppProtocolUnificationHandler.class);

    private final Holder holder;
    private final AppChannelStateHandler appChannelStateHandler;
    private final RegisterHandler registerHandler;
    private final AppLoginHandler appLoginHandler;
    private final AppShareLoginHandler appShareLoginHandler;
    private final UserNotLoggedHandler userNotLoggedHandler;
    private final GetServerHandler getServerHandler;

    private final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler;
    private final LetsEncryptHandler letsEncryptHandler;

    public HttpAndAppProtocolUnificationHandler(Holder holder,
                                                AppChannelStateHandler appChannelStateHandler,
                                                RegisterHandler registerHandler,
                                                AppLoginHandler appLoginHandler,
                                                AppShareLoginHandler appShareLoginHandler,
                                                UserNotLoggedHandler userNotLoggedHandler,
                                                GetServerHandler getServerHandler,
                                                HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler,
                                                LetsEncryptHandler letsEncryptHandler) {
        this.holder = holder;
        this.appChannelStateHandler = appChannelStateHandler;
        this.registerHandler = registerHandler;
        this.appLoginHandler = appLoginHandler;
        this.appShareLoginHandler = appShareLoginHandler;
        this.userNotLoggedHandler = userNotLoggedHandler;
        this.getServerHandler = getServerHandler;

        this.httpAndWebSocketUnificatorHandler = httpAndWebSocketUnificatorHandler;
        this.letsEncryptHandler = letsEncryptHandler;

        log.debug("app.socket.idle.timeout = 600 for new protocol");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Will use the first 2 bytes to detect a protocol.
        if (in.readableBytes() < 2) {
            return;
        }

        int readerIndex = in.readerIndex();
        short magic1 = in.getUnsignedByte(readerIndex);
        short magic2 = in.getUnsignedByte(readerIndex + 1);

        ChannelPipeline pipeline = ctx.pipeline();
        buildPipeline(pipeline, magic1, magic2).remove(this);
    }

    private ChannelPipeline buildPipeline(ChannelPipeline pipeline, short magic1, short magic2) {
        if (HttpUtil.isHttp(magic1, magic2)) {
            return buildHTTPipeline(pipeline);
        }
        return buildBlynkPipeline(pipeline);
    }

    private ChannelPipeline buildHTTPipeline(ChannelPipeline pipeline) {
        log.trace("HTTP connection detected.", pipeline.channel());
        return pipeline
                .addLast("HttpsServerCodec", new HttpServerCodec())
                .addLast("HttpsServerKeepAlive", new HttpServerKeepAliveHandler())
                .addLast("HttpsObjectAggregator",
                        new HttpObjectAggregator(holder.limits.webRequestMaxSize, true))
                .addLast(letsEncryptHandler)
                .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                .addLast("HttpUrlMapper", new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                .addLast("HttpStaticFile", new StaticFileHandler(holder.props, new StaticFile("/static"),
                        new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")))
                .addLast("HttpsWebSocketUnificator", httpAndWebSocketUnificatorHandler)
                .addLast(new OTAHandler(holder,
                        httpAndWebSocketUnificatorHandler.rootPath + "/ota/start", "/static/ota"));
    }

    private ChannelPipeline buildBlynkPipeline(ChannelPipeline pipeline) {
        log.trace("Blynk protocol connection detected.", pipeline.channel());
        return pipeline
                .addLast("AReadTimeout", new IdleStateHandler(600, 0, 0))
                .addLast("AChannelState", appChannelStateHandler)
                .addLast("AMessageDecoder", new AppMessageDecoder(holder.stats))
                .addLast("AMessageEncoder", new AppMessageEncoder(holder.stats))
                .addLast("AGetServer", getServerHandler)
                .addLast("ARegister", registerHandler)
                .addLast("ALogin", appLoginHandler)
                .addLast("AShareLogin", appShareLoginHandler)
                .addLast("ANotLogged", userNotLoggedHandler);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleUnexpectedException(ctx, cause);
    }
}
