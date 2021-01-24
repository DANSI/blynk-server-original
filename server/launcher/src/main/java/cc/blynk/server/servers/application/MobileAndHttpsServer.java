package cc.blynk.server.servers.application;

import cc.blynk.core.http.handlers.CookieBasedUrlReWriterHandler;
import cc.blynk.core.http.handlers.NoCacheStaticFile;
import cc.blynk.core.http.handlers.NoMatchHandler;
import cc.blynk.core.http.handlers.OTAHandler;
import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.UploadHandler;
import cc.blynk.core.http.handlers.url.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.handlers.IpFilterHandler;
import cc.blynk.server.admin.http.logic.ConfigsLogic;
import cc.blynk.server.admin.http.logic.HardwareStatsLogic;
import cc.blynk.server.admin.http.logic.OTALogic;
import cc.blynk.server.admin.http.logic.StatsLogic;
import cc.blynk.server.admin.http.logic.UsersLogic;
import cc.blynk.server.api.http.handlers.BaseHttpAndBlynkUnificationHandler;
import cc.blynk.server.api.http.handlers.BaseWebSocketUnificator;
import cc.blynk.server.api.http.logic.HttpAPILogic;
import cc.blynk.server.api.http.logic.ResetPasswordHttpLogic;
import cc.blynk.server.api.http.logic.business.AdminAuthHandler;
import cc.blynk.server.api.http.logic.business.AuthCookieHandler;
import cc.blynk.server.api.websockets.handlers.WSHandler;
import cc.blynk.server.api.websockets.handlers.WSWrapperEncoder;
import cc.blynk.server.application.handlers.main.MobileChannelStateHandler;
import cc.blynk.server.application.handlers.main.MobileResetPasswordHandler;
import cc.blynk.server.application.handlers.main.auth.MobileGetServerHandler;
import cc.blynk.server.application.handlers.main.auth.MobileLoginHandler;
import cc.blynk.server.application.handlers.main.auth.MobileRegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.MobileShareLoginHandler;
import cc.blynk.server.common.handlers.AlreadyLoggedHandler;
import cc.blynk.server.common.handlers.UserNotLoggedHandler;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.decoders.MobileMessageDecoder;
import cc.blynk.server.core.protocol.handlers.decoders.WSMessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.protocol.handlers.encoders.MobileMessageEncoder;
import cc.blynk.server.core.protocol.handlers.encoders.WSMessageEncoder;
import cc.blynk.server.hardware.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.NumberUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import static cc.blynk.core.http.Response.redirect;
import static cc.blynk.utils.StringUtils.BLYNK_LANDING;
import static cc.blynk.utils.StringUtils.WEBSOCKETS_PATH;
import static cc.blynk.utils.StringUtils.WEBSOCKET_PATH;
import static cc.blynk.utils.StringUtils.WEBSOCKET_WEB_PATH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class MobileAndHttpsServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public MobileAndHttpsServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        var appChannelStateHandler = new MobileChannelStateHandler(holder.sessionDao);
        var registerHandler = new MobileRegisterHandler(holder);
        MobileLoginHandler appLoginHandler = new MobileLoginHandler(holder);
        var appShareLoginHandler = new MobileShareLoginHandler(holder);
        var userNotLoggedHandler = new UserNotLoggedHandler();
        var getServerHandler = new MobileGetServerHandler(holder);
        var resetPasswordHandler = new MobileResetPasswordHandler(holder);

        var hardwareIdleTimeout = holder.limits.hardwareIdleTimeout;
        var appIdleTimeout = holder.limits.appIdleTimeout;

        var hardwareChannelStateHandler = new HardwareChannelStateHandler(holder);
        var hardwareLoginHandler = new HardwareLoginHandler(holder, port);

        var rootPath = holder.props.getAdminRootPath();

        var ipFilterHandler = new IpFilterHandler(
                holder.props.getCommaSeparatedValueAsArray("allowed.administrator.ips"));

        var stats = holder.stats;

        //http API handlers
        var resetPasswordLogic = new ResetPasswordHttpLogic(holder);
        var httpAPILogic = new HttpAPILogic(holder);
        var noMatchHandler = new NoMatchHandler();
        var webSocketHandler = new WSHandler(stats);
        var webSocketWrapperEncoder = new WSWrapperEncoder();

        var webAppMessageEncoder = new WSMessageEncoder();

        //admin API handlers
        var otaLogic = new OTALogic(holder, rootPath);
        var usersLogic = new UsersLogic(holder, rootPath);
        var statsLogic = new StatsLogic(holder, rootPath);
        var configsLogic = new ConfigsLogic(holder, rootPath);
        var hardwareStatsLogic = new HardwareStatsLogic(holder, rootPath);
        var adminAuthHandler = new AdminAuthHandler(holder, rootPath);
        var authCookieHandler = new AuthCookieHandler(holder.sessionDao);
        var cookieBasedUrlReWriterHandler =
                new CookieBasedUrlReWriterHandler(rootPath, "/static/admin.html", "/static/login.html");

        var alreadyLoggedHandler = new AlreadyLoggedHandler();
        int hardTimeoutSecs = NumberUtil.calcHeartbeatTimeout(holder.limits.hardwareIdleTimeout);

        var baseWebSocketUnificator = new BaseWebSocketUnificator() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                var req = (FullHttpRequest) msg;
                var uri = req.uri();

                log.trace("In http and websocket unificator handler.");
                if (uri.equals("/")) {
                    //for local server do redirect to admin page
                    try {
                        ctx.writeAndFlush(redirect(holder.props.isLocalRegion() ? rootPath : BLYNK_LANDING));
                    } finally {
                        req.release();
                    }
                    return;
                } else if (uri.startsWith(rootPath)) {
                    initAdminPipeline(ctx);
                } else if (uri.startsWith(WEBSOCKET_PATH)) {
                    initWebSocketPipeline(ctx, WEBSOCKETS_PATH);
                } else if (uri.equals(WEBSOCKET_WEB_PATH)) {
                    initWebDashboardSocket(ctx);
                } else {
                    initHttpPipeline(ctx);
                }

                ctx.fireChannelRead(msg);
            }

            private void initAdminPipeline(ChannelHandlerContext ctx) {
                if (!ipFilterHandler.accept(ctx)) {
                    ctx.close();
                    return;
                }

                var pipeline = ctx.pipeline();

                pipeline.addLast(new UploadHandler(holder.props.jarPath, "/upload", "/static/ota"))
                        .addLast(new OTAHandler(holder, rootPath + "/ota/start", "/static/ota"))
                        .addLast(adminAuthHandler)
                        .addLast(authCookieHandler)
                        .addLast(cookieBasedUrlReWriterHandler);

                pipeline.remove(StaticFileHandler.class);
                pipeline.addLast(new StaticFileHandler(holder.props, new NoCacheStaticFile("/static")))
                        .addLast(otaLogic)
                        .addLast(usersLogic)
                        .addLast(statsLogic)
                        .addLast(configsLogic)
                        .addLast(hardwareStatsLogic)
                        .addLast(httpAPILogic)
                        .addLast(noMatchHandler)
                        .remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized admin pipeline. {}", ctx.pipeline().names());
                }
            }

            private void initHttpPipeline(ChannelHandlerContext ctx) {
                ctx.pipeline()
                        .addLast(resetPasswordLogic)
                        .addLast(httpAPILogic)
                        .addLast(noMatchHandler)
                        .remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized https pipeline. {}", ctx.pipeline().names());
                }
            }

            private void initWebDashboardSocket(ChannelHandlerContext ctx) {
                var pipeline = ctx.pipeline();

                //websockets specific handlers
                pipeline.addFirst("AChannelState", appChannelStateHandler)
                        .addFirst("AReadTimeout", new IdleStateHandler(appIdleTimeout, 0, 0))
                        .addLast("WSWebSocketServerProtocolHandler",
                        new WebSocketServerProtocolHandler(WEBSOCKET_WEB_PATH))
                        .addLast("WSMessageDecoder", new WSMessageDecoder(stats, holder.limits))
                        .addLast("WSMessageEncoder", webAppMessageEncoder)
                        .addLast("AGetServer", getServerHandler)
                        .addLast("ALogin", appLoginHandler)
                        .addLast("ANotLogged", userNotLoggedHandler);
                pipeline.remove(ChunkedWriteHandler.class);
                pipeline.remove(UrlReWriterHandler.class);
                pipeline.remove(StaticFileHandler.class);
                pipeline.remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized web dashboard pipeline. {}", ctx.pipeline().names());
                }
            }

            private void initWebSocketPipeline(ChannelHandlerContext ctx, String websocketPath) {
                var pipeline = ctx.pipeline();

                //websockets specific handlers
                pipeline.addFirst("WSIdleStateHandler", new IdleStateHandler(hardwareIdleTimeout, 0, 0))
                        .addLast("WSChannelState", hardwareChannelStateHandler)
                        .addLast("WSWebSocketServerProtocolHandler",
                        new WebSocketServerProtocolHandler(websocketPath, true))
                        .addLast("WSWebSocket", webSocketHandler)
                        .addLast("WSMessageDecoder", new MessageDecoder(stats, holder.limits))
                        .addLast("WSSocketWrapper", webSocketWrapperEncoder)
                        .addLast("WSMessageEncoder", new MessageEncoder(stats))
                        .addLast("WSLogin", hardwareLoginHandler)
                        .addLast("WSNotLogged", alreadyLoggedHandler);
                pipeline.remove(ChunkedWriteHandler.class);
                pipeline.remove(UrlReWriterHandler.class);
                pipeline.remove(StaticFileHandler.class);
                pipeline.remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized secured hardware websocket pipeline. {}", ctx.pipeline().names());
                }
            }
        };

        channelInitializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                .addLast(holder.sslContextHolder.sslCtx.newHandler(ch.alloc()))
                .addLast(new BaseHttpAndBlynkUnificationHandler() {
                    @Override
                    public ChannelPipeline buildHttpPipeline(ChannelPipeline pipeline) {
                        log.trace("HTTPS connection detected.", pipeline.channel());
                        return pipeline
                                .addLast("HttpsServerCodec", new HttpServerCodec())
                                .addLast("HttpsServerKeepAlive", new HttpServerKeepAliveHandler())
                                .addLast("HttpsObjectAggregator",
                                        new HttpObjectAggregator(holder.limits.webRequestMaxSize, true))
                                .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                                .addLast("HttpUrlMapper",
                                        new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                                .addLast("HttpStaticFile",
                                        new StaticFileHandler(holder.props, new StaticFile("/static"),
                                                new StaticFileEdsWith(FileUtils.CSV_DIR, ".gz"),
                                                new StaticFileEdsWith(FileUtils.CSV_DIR, ".zip")))
                                .addLast("HttpsWebSocketUnificator", baseWebSocketUnificator);
                    }

                    @Override
                    public ChannelPipeline buildAppPipeline(ChannelPipeline pipeline) {
                        log.trace("Blynk app protocol connection detected.", pipeline.channel());
                        return pipeline
                                .addFirst("AChannelState", appChannelStateHandler)
                                .addFirst("AReadTimeout", new IdleStateHandler(appIdleTimeout, 0, 0))
                                .addLast("AMessageDecoder", new MobileMessageDecoder(holder.stats, holder.limits))
                                .addLast("AMessageEncoder", new MobileMessageEncoder(holder.stats))
                                .addLast("AGetServer", getServerHandler)
                                .addLast("ARegister", registerHandler)
                                .addLast("ALogin", appLoginHandler)
                                .addLast("AResetPass", resetPasswordHandler)
                                .addLast("AShareLogin", appShareLoginHandler)
                                .addLast("ANotLogged", userNotLoggedHandler);
                    }

                    @Override
                    public ChannelPipeline buildHardwarePipeline(ChannelPipeline pipeline) {
                        log.trace("Blynk ssl hardware protocol connection detected.", pipeline.channel());
                        return pipeline
                                .addFirst("H_IdleStateHandler",
                                        new IdleStateHandler(hardTimeoutSecs, 0, 0))
                                .addLast("H_ChannelState", hardwareChannelStateHandler)
                                .addLast("H_MessageDecoder", new MessageDecoder(holder.stats, holder.limits))
                                .addLast("H_MessageEncoder", new MessageEncoder(holder.stats))
                                .addLast("H_Login", hardwareLoginHandler)
                                .addLast("H_AlreadyLogged", alreadyLoggedHandler);
                    }
                });
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTPS API, WebSockets and Admin page";
    }

    @Override
    public ChannelFuture close() {
        System.out.println("Shutting down HTTPS API, WebSockets and Admin server...");
        return super.close();
    }

}
