package cc.blynk.server.api.http.handlers;

import cc.blynk.core.http.handlers.*;
import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.handlers.IpFilterHandler;
import cc.blynk.server.admin.http.logic.ConfigsLogic;
import cc.blynk.server.admin.http.logic.HardwareStatsLogic;
import cc.blynk.server.admin.http.logic.StatsLogic;
import cc.blynk.server.admin.http.logic.UsersLogic;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.api.http.logic.HttpAPILogic;
import cc.blynk.server.api.http.logic.ResetPasswordLogic;
import cc.blynk.server.api.http.logic.business.AdminAuthHandler;
import cc.blynk.server.api.http.logic.business.AuthCookieHandler;
import cc.blynk.server.api.http.logic.ide.IDEAuthLogic;
import cc.blynk.server.api.websockets.handlers.WebSocketHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketWrapperEncoder;
import cc.blynk.server.api.websockets.handlers.WebSocketsGenericLoginHandler;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
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

    private final static String BLYNK_LANDING = "http://www.blynk.cc";

    private final String region;
    private final GlobalStats stats;

    private final WebSocketsGenericLoginHandler genericLoginHandler;
    private final String adminRootPath;
    private final boolean isUnpacked;
    private final IpFilterHandler ipFilterHandler;
    private final AuthCookieHandler authCookieHandler;

    private final ResetPasswordLogic resetPasswordLogic;
    private final HttpAPILogic httpAPILogic;
    private final IDEAuthLogic ideAuthLogic;
    private final NoMatchHandler noMatchHandler;

    private final UsersLogic usersLogic;
    private final StatsLogic statsLogic;
    private final ConfigsLogic configsLogic;
    private final HardwareStatsLogic hardwareStatsLogic;
    private final AdminAuthHandler adminAuthHandler;
    private final  CookieBasedUrlReWriterHandler cookieBasedUrlReWriterHandler;

    public HttpAndWebSocketUnificatorHandler(Holder holder, int port, String adminRootPath, boolean isUnpacked) {
        this.region = holder.region;
        this.stats = holder.stats;
        this.genericLoginHandler = new WebSocketsGenericLoginHandler(holder, port);
        this.adminRootPath = adminRootPath;
        this.isUnpacked = isUnpacked;
        this.ipFilterHandler = new IpFilterHandler(holder.props.getCommaSeparatedValueAsArray("allowed.administrator.ips"));

        //http API handlers
        this.resetPasswordLogic = new ResetPasswordLogic(holder);
        this.httpAPILogic = new HttpAPILogic(holder);
        this.ideAuthLogic = new IDEAuthLogic(holder);
        this.noMatchHandler = new NoMatchHandler();

        //admin API handlers
        this.usersLogic = new UsersLogic(holder, adminRootPath);
        this.statsLogic = new StatsLogic(holder, adminRootPath);
        this.configsLogic = new ConfigsLogic(holder, adminRootPath);
        this.hardwareStatsLogic = new HardwareStatsLogic(holder, adminRootPath);
        this.adminAuthHandler = new AdminAuthHandler(holder, adminRootPath);
        this.authCookieHandler = new AuthCookieHandler(holder.sessionDao);
        this.cookieBasedUrlReWriterHandler = new CookieBasedUrlReWriterHandler(adminRootPath, "/static/admin/admin.html", "/static/admin/login.html");
    }

    public HttpAndWebSocketUnificatorHandler(Holder holder, int port, String adminRootPath) {
        this(holder, port, adminRootPath, false);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final FullHttpRequest req = (FullHttpRequest) msg;
        String uri = req.uri();

        if (uri.equals("/")) {
            //for local server do redirect to admin page
            if (region.equals("local")) {
                ctx.writeAndFlush(redirect(adminRootPath));
            } else {
                ctx.writeAndFlush(redirect(BLYNK_LANDING));
            }
        } else if (uri.startsWith(adminRootPath) || uri.startsWith("/static/admin")) {
            initAdminPipeline(ctx, msg);
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

    private void initAdminPipeline(ChannelHandlerContext ctx, Object msg) {
        if (isIpNotAllowed(ctx)) {
            ctx.close();
            return;
        }

        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast(new ChunkedWriteHandler());

        pipeline.addLast(adminAuthHandler);
        pipeline.addLast(authCookieHandler);
        pipeline.addLast(cookieBasedUrlReWriterHandler);

        pipeline.addLast(new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"));
        pipeline.addLast(new StaticFileHandler(isUnpacked, new StaticFile("/static", false)));

        pipeline.addLast(usersLogic);
        pipeline.addLast(statsLogic);
        pipeline.addLast(configsLogic);
        pipeline.addLast(hardwareStatsLogic);

        pipeline.addLast(resetPasswordLogic);
        pipeline.addLast(httpAPILogic);
        pipeline.addLast(noMatchHandler);
        pipeline.remove(this);
    }

    private void initHttpPipeline(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast("HttpChunkedWrite", new ChunkedWriteHandler());
        pipeline.addLast("HttpUrlMapper", new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"));
        pipeline.addLast("HttpStaticFile", new StaticFileHandler(isUnpacked, new StaticFile("/static"),
                                           new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")));

        pipeline.addLast(resetPasswordLogic);
        pipeline.addLast(httpAPILogic);
        pipeline.addLast(ideAuthLogic);

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
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
