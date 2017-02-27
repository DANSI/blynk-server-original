package cc.blynk.server.api.http;

import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.UrlMapperHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketWrapperEncoder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.common.HardwareNotLoggedHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.utils.FileUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * Utility handler used to define what protocol should be handled
 * on same port : http or websockets.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.02.17.
 */
@ChannelHandler.Sharable
public class HttpAndWebSocketUnificatorHandler extends ChannelInboundHandlerAdapter {

    private final GlobalStats stats;
    private final int hardTimeoutSecs;
    private final HardwareLoginHandler hardwareLoginHandler;
    private final HardwareChannelStateHandler hardwareChannelStateHandler;
    private final TokenManager tokenManager;
    private final SessionDao sessionDao;

    public HttpAndWebSocketUnificatorHandler(Holder holder, int port) {
        this.stats = holder.stats;
        this.hardTimeoutSecs = holder.props.getIntProperty("hard.socket.idle.timeout", 0);
        this.hardwareLoginHandler = new HardwareLoginHandler(holder, port);
        this.hardwareChannelStateHandler = new HardwareChannelStateHandler(holder.sessionDao, holder.gcmWrapper);
        this.tokenManager = holder.tokenManager;
        this.sessionDao = holder.sessionDao;

        //HandlerRegistry.register(new HttpBusinessAPILogic(holder));
        //final String businessRootPath = holder.props.getProperty("business.rootPath", "/business");
        //final SessionHolder sessionHolder = new SessionHolder();
        //HandlerRegistry.register(businessRootPath, new BusinessAuthLogic(holder.userDao, holder.sessionDao, holder.fileManager, sessionHolder));

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final FullHttpRequest req = (FullHttpRequest) msg;
        if (req.uri().startsWith(HttpAPIServer.WEBSOCKET_PATH)) {
            initWebSocketPipeline(ctx, HttpAPIServer.WEBSOCKET_PATH);
        } else {
            initHttpPipeline(ctx);
        }

        ctx.fireChannelRead(msg);
    }

    private void initHttpPipeline(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast("HttpChunkedWrite", new ChunkedWriteHandler());
        pipeline.addLast("HttpUrlMapper", new UrlMapperHandler("/favicon.ico", "/static/favicon.ico"));
        pipeline.addLast("HttpStaticFile", new StaticFileHandler(true,
                new StaticFile("/static"), new StaticFileEdsWith(FileUtils.CSV_DIR, ".csv.gz")));
        pipeline.addLast("HttpHandler", new HttpHandler(tokenManager, sessionDao, stats));


        //pipeline.addLast("HttpsAuthCookie", new AuthCookieHandler(businessRootPath, sessionHolder));
        //pipeline.addLast("HttpsUrlMapper", new UrlMapperHandler(businessRootPath, "/static/business/business.html"));

        pipeline.remove(this);
    }

    private void initWebSocketPipeline(ChannelHandlerContext ctx, String websocketPath) {
        ChannelPipeline pipeline = ctx.pipeline();

        if (hardTimeoutSecs > 0) {
            pipeline.addFirst("WSReadTimeout", new ReadTimeoutHandler(hardTimeoutSecs));
        }

        //websockets specific handlers
        pipeline.addLast("WSWebSocketServerProtocolHandler", new WebSocketServerProtocolHandler(websocketPath, true));
        pipeline.addLast("WSWebSocket", new WebSocketHandler(stats));

        //hardware handlers
        pipeline.addLast("WSChannelState", hardwareChannelStateHandler);
        pipeline.addLast("WSMessageDecoder", new MessageDecoder(stats));
        pipeline.addLast("WSSocketWrapper", new WebSocketWrapperEncoder());
        pipeline.addLast("WSMessageEncoder", new MessageEncoder(stats));
        pipeline.addLast("WSLogin", hardwareLoginHandler);
        pipeline.addLast("WSNotLogged", new HardwareNotLoggedHandler());
        pipeline.remove(this);
    }
}
