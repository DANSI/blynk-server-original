package cc.blynk.server.api.http.handlers;

import cc.blynk.core.http.handlers.OTAHandler;
import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.url.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.GetServerHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.protocol.handlers.decoders.AppMessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.AppMessageEncoder;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 18.01.18.
 */
public class HttpsAndAppProtocolUnificationHandler extends BaseHttpAndBlynkUnificationHandler {

    private final Holder holder;
    private final AppChannelStateHandler appChannelStateHandler;
    private final RegisterHandler registerHandler;
    private final AppLoginHandler appLoginHandler;
    private final AppShareLoginHandler appShareLoginHandler;
    private final UserNotLoggedHandler userNotLoggedHandler;
    private final GetServerHandler getServerHandler;

    private final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler;

    public HttpsAndAppProtocolUnificationHandler(Holder holder,
                                                 AppChannelStateHandler appChannelStateHandler,
                                                 RegisterHandler registerHandler,
                                                 AppLoginHandler appLoginHandler,
                                                 AppShareLoginHandler appShareLoginHandler,
                                                 UserNotLoggedHandler userNotLoggedHandler,
                                                 GetServerHandler getServerHandler,
                                                 HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler) {
        this.holder = holder;
        this.appChannelStateHandler = appChannelStateHandler;
        this.registerHandler = registerHandler;
        this.appLoginHandler = appLoginHandler;
        this.appShareLoginHandler = appShareLoginHandler;
        this.userNotLoggedHandler = userNotLoggedHandler;
        this.getServerHandler = getServerHandler;

        this.httpAndWebSocketUnificatorHandler = httpAndWebSocketUnificatorHandler;

        log.debug("app.socket.idle.timeout = 600 for new protocol");
    }

    @Override
    public ChannelPipeline buildHttpPipeline(ChannelPipeline pipeline) {
        log.trace("HTTPS connection detected.", pipeline.channel());
        return pipeline
                .addLast("HttpsServerCodec", new HttpServerCodec())
                .addLast("HttpsServerKeepAlive", new HttpServerKeepAliveHandler())
                .addLast("HttpsObjectAggregator",
                        new HttpObjectAggregator(holder.limits.webRequestMaxSize, true))
                .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                .addLast("HttpUrlMapper", new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                .addLast("HttpStaticFile", new StaticFileHandler(holder.props, new StaticFile("/static"),
                        new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")))
                .addLast("HttpsWebSocketUnificator", httpAndWebSocketUnificatorHandler)
                .addLast(new OTAHandler(holder,
                        httpAndWebSocketUnificatorHandler.rootPath + "/ota/start", "/static/ota"));
    }

    @Override
    public ChannelPipeline buildBlynkPipeline(ChannelPipeline pipeline) {
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
