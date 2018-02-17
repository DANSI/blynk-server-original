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
import cc.blynk.server.api.http.logic.ResetPasswordLogic;
import cc.blynk.server.api.http.logic.business.AdminAuthHandler;
import cc.blynk.server.api.http.logic.business.AuthCookieHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketWrapperEncoder;
import cc.blynk.server.api.websockets.handlers.WebSocketsGenericLoginHandler;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.GetServerHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.protocol.handlers.decoders.AppMessageDecoder;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.AppMessageEncoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.server.servers.BaseServer;
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
import static cc.blynk.utils.StringUtils.WEBSOCKET_PATH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class AppAndHttpsServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AppAndHttpsServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        AppChannelStateHandler appChannelStateHandler = new AppChannelStateHandler(holder.sessionDao);
        RegisterHandler registerHandler = new RegisterHandler(holder);
        AppLoginHandler appLoginHandler = new AppLoginHandler(holder);
        AppShareLoginHandler appShareLoginHandler = new AppShareLoginHandler(holder);
        UserNotLoggedHandler userNotLoggedHandler = new UserNotLoggedHandler();
        GetServerHandler getServerHandler = new GetServerHandler(holder);

        String rootPath = holder.props.getAdminRootPath();

        IpFilterHandler ipFilterHandler = new IpFilterHandler(
                holder.props.getCommaSeparatedValueAsArray("allowed.administrator.ips"));

        GlobalStats stats = holder.stats;
        WebSocketsGenericLoginHandler genericLoginHandler = new WebSocketsGenericLoginHandler(holder, port);

        //http API handlers
        ResetPasswordLogic resetPasswordLogic = new ResetPasswordLogic(holder);
        HttpAPILogic httpAPILogic = new HttpAPILogic(holder);
        NoMatchHandler noMatchHandler = new NoMatchHandler();

        //admin API handlers
        OTALogic otaLogic = new OTALogic(holder, rootPath);
        UsersLogic usersLogic = new UsersLogic(holder, rootPath);
        StatsLogic statsLogic = new StatsLogic(holder, rootPath);
        ConfigsLogic configsLogic = new ConfigsLogic(holder, rootPath);
        HardwareStatsLogic hardwareStatsLogic = new HardwareStatsLogic(holder, rootPath);
        AdminAuthHandler adminAuthHandler = new AdminAuthHandler(holder, rootPath);
        AuthCookieHandler authCookieHandler = new AuthCookieHandler(holder.sessionDao);
        CookieBasedUrlReWriterHandler cookieBasedUrlReWriterHandler =
                new CookieBasedUrlReWriterHandler(rootPath, "/static/admin.html", "/static/login.html");

        BaseWebSocketUnificator baseWebSocketUnificator = new BaseWebSocketUnificator() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                FullHttpRequest req = (FullHttpRequest) msg;
                String uri = req.uri();

                log.debug("In http and websocket unificator handler.");
                if (uri.equals("/")) {
                    //for local server do redirect to admin page
                    try {
                        ctx.writeAndFlush(redirect(holder.isLocalRegion() ? rootPath : BLYNK_LANDING));
                    } finally {
                        req.release();
                    }
                    return;
                } else if (uri.startsWith(rootPath)) {
                    initAdminPipeline(ctx);
                } else if (uri.startsWith(WEBSOCKET_PATH)) {
                    initWebSocketPipeline(ctx, WEBSOCKET_PATH);
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

                ChannelPipeline pipeline = ctx.pipeline();

                pipeline.addLast(new UploadHandler(holder.props.jarPath, "/upload", "/static/ota"))
                        .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                        .addLast("HttpUrlMapper",
                                new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                        .addLast("HttpStaticFile",
                                new StaticFileHandler(holder.props, new StaticFile("/static"),
                                        new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")))
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
                        .addLast(resetPasswordLogic)
                        .addLast(httpAPILogic)
                        .addLast(noMatchHandler)
                        .remove(this);
            }

            private void initHttpPipeline(ChannelHandlerContext ctx) {
                ctx.pipeline()
                        .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                        .addLast("HttpUrlMapper",
                                new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                        .addLast("HttpStaticFile",
                                new StaticFileHandler(holder.props, new StaticFile("/static"),
                                        new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")))
                        .addLast(resetPasswordLogic)
                        .addLast(httpAPILogic)
                        .addLast(noMatchHandler)
                        .remove(this);
            }

            private void initWebSocketPipeline(ChannelHandlerContext ctx, String websocketPath) {
                ChannelPipeline pipeline = ctx.pipeline();

                //websockets specific handlers
                pipeline.addLast("WSWebSocketServerProtocolHandler",
                        new WebSocketServerProtocolHandler(websocketPath, true));
                pipeline.addLast("WSWebSocket", new WebSocketHandler(stats));
                pipeline.addLast("WSMessageDecoder", new MessageDecoder(stats));
                pipeline.addLast("WSSocketWrapper", new WebSocketWrapperEncoder());
                pipeline.addLast("WSMessageEncoder", new MessageEncoder(stats));
                pipeline.addLast("WSWebSocketGenericLoginHandler", genericLoginHandler);
                pipeline.remove(this);
            }
        };

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
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
                                .addLast("HttpsWebSocketUnificator", baseWebSocketUnificator);
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
    public void close() {
        System.out.println("Shutting down HTTPS API, WebSockets and Admin server...");
        super.close();
    }

}
