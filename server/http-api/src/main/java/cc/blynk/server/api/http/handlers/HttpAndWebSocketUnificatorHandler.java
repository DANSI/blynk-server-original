package cc.blynk.server.api.http.handlers;

import cc.blynk.core.http.handlers.*;
import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.handlers.IpFilterHandler;
import cc.blynk.server.admin.http.logic.*;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.api.http.logic.HttpAPILogic;
import cc.blynk.server.api.http.logic.ResetPasswordLogic;
import cc.blynk.server.api.http.logic.business.AdminAuthHandler;
import cc.blynk.server.api.http.logic.business.AuthCookieHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketWrapperEncoder;
import cc.blynk.server.api.websockets.handlers.WebSocketsGenericLoginHandler;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.utils.ServerProperties;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.net.InetSocketAddress;

import static cc.blynk.core.http.Response.redirect;

/**
 * Utility handler used to define what protocol should be handled
 * on same port : http or websockets.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.02.17.
 */
@ChannelHandler.Sharable
public class HttpAndWebSocketUnificatorHandler extends ChannelInboundHandlerAdapter implements DefaultExceptionHandler {

    private final static String BLYNK_LANDING = "https://www.blynk.cc";

    private final String region;
    private final GlobalStats stats;

    private final WebSocketsGenericLoginHandler genericLoginHandler;
    private final String rootPath;
    private final IpFilterHandler ipFilterHandler;
    private final AuthCookieHandler authCookieHandler;

    private final ResetPasswordLogic resetPasswordLogic;
    private final HttpAPILogic httpAPILogic;
    private final NoMatchHandler noMatchHandler;

    private final OTALogic otaLogic;
    private final UsersLogic usersLogic;
    private final StatsLogic statsLogic;
    private final ConfigsLogic configsLogic;
    private final HardwareStatsLogic hardwareStatsLogic;
    private final AdminAuthHandler adminAuthHandler;
    private final CookieBasedUrlReWriterHandler cookieBasedUrlReWriterHandler;

    private final ServerProperties props;

    public HttpAndWebSocketUnificatorHandler(Holder holder, int port, String rootPath) {
        this.region = holder.region;
        this.stats = holder.stats;
        this.genericLoginHandler = new WebSocketsGenericLoginHandler(holder, port);
        this.rootPath = rootPath;
        this.props = holder.props;
        this.ipFilterHandler = new IpFilterHandler(holder.props.getCommaSeparatedValueAsArray("allowed.administrator.ips"));

        //http API handlers
        this.resetPasswordLogic = new ResetPasswordLogic(holder);
        this.httpAPILogic = new HttpAPILogic(holder);
        this.noMatchHandler = new NoMatchHandler();

        //admin API handlers
        this.otaLogic = new OTALogic(holder, rootPath);
        this.usersLogic = new UsersLogic(holder, rootPath);
        this.statsLogic = new StatsLogic(holder, rootPath);
        this.configsLogic = new ConfigsLogic(holder, rootPath);
        this.hardwareStatsLogic = new HardwareStatsLogic(holder, rootPath);
        this.adminAuthHandler = new AdminAuthHandler(holder, rootPath);
        this.authCookieHandler = new AuthCookieHandler(holder.sessionDao);
        this.cookieBasedUrlReWriterHandler = new CookieBasedUrlReWriterHandler(rootPath, "/static/admin.html", "/static/login.html");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest req = (FullHttpRequest) msg;
        String uri = req.uri();

        log.debug("In http and websocket unificator handler.");
        if (uri.equals("/")) {
            //for local server do redirect to admin page
            try {
                if (region.equals("local")) {
                    ctx.writeAndFlush(redirect(rootPath));
                } else {
                    ctx.writeAndFlush(redirect(BLYNK_LANDING));
                }
            } finally {
                req.release();
            }
            return;
        } else if (uri.startsWith(rootPath)) {
            initAdminPipeline(ctx);
        } else if (req.uri().startsWith(HttpAPIServer.WEBSOCKET_PATH)) {
            initWebSocketPipeline(ctx, HttpAPIServer.WEBSOCKET_PATH);
        } else {
            initHttpPipeline(ctx);
        }

        ctx.fireChannelRead(msg);
    }

    private boolean isIpNotAllowed(ChannelHandlerContext ctx) {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        return !ipFilterHandler.accept(ctx, remoteAddress);
    }

    private void initAdminPipeline(ChannelHandlerContext ctx) {
        if (isIpNotAllowed(ctx)) {
            ctx.close();
            return;
        }

        ChannelPipeline pipeline = ctx.pipeline();

        pipeline.addLast(new UploadHandler(props.jarPath, "/upload", "/static/ota"));
        pipeline.addLast(adminAuthHandler);
        pipeline.addLast(authCookieHandler);
        pipeline.addLast(cookieBasedUrlReWriterHandler);

        pipeline.remove(StaticFileHandler.class);
        pipeline.addLast(new StaticFileHandler(props, new StaticFile("/static")));

        pipeline.addLast(otaLogic);
        pipeline.addLast(usersLogic);
        pipeline.addLast(statsLogic);
        pipeline.addLast(configsLogic);
        pipeline.addLast(hardwareStatsLogic);

        pipeline.addLast(resetPasswordLogic);
        pipeline.addLast(httpAPILogic);
        pipeline.addLast(noMatchHandler);
        pipeline.remove(this);
        pipeline.remove(LetsEncryptHandler.class);
    }

    private void initHttpPipeline(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast(resetPasswordLogic);
        pipeline.addLast(httpAPILogic);

        pipeline.addLast(noMatchHandler);
        pipeline.remove(this);
    }

    private void initWebSocketPipeline(ChannelHandlerContext ctx, String websocketPath) {
        ChannelPipeline pipeline = ctx.pipeline();

        //websockets specific handlers
        pipeline.addLast("WSWebSocketServerProtocolHandler", new WebSocketServerProtocolHandler(websocketPath, true));
        pipeline.addLast("WSWebSocket", new WebSocketHandler(stats));
        pipeline.addLast("WSMessageDecoder", new MessageDecoder(stats));
        pipeline.addLast("WSSocketWrapper", new WebSocketWrapperEncoder());
        pipeline.addLast("WSMessageEncoder", new MessageEncoder(stats));
        pipeline.addLast("WSWebSocketGenericLoginHandler", genericLoginHandler);
        pipeline.remove(this);
        pipeline.remove(ChunkedWriteHandler.class);
        pipeline.remove(UrlReWriterHandler.class);
        pipeline.remove(StaticFileHandler.class);
        pipeline.remove(LetsEncryptHandler.class);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
