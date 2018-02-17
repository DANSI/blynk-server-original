package cc.blynk.server.servers.hardware;

import cc.blynk.core.http.handlers.NoMatchHandler;
import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.url.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.BaseHttpAndBlynkUnificationHandler;
import cc.blynk.server.api.http.handlers.BaseWebSocketUnificator;
import cc.blynk.server.api.http.handlers.LetsEncryptHandler;
import cc.blynk.server.api.http.logic.HttpAPILogic;
import cc.blynk.server.api.http.logic.ResetPasswordLogic;
import cc.blynk.server.api.websockets.handlers.WebSocketHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketWrapperEncoder;
import cc.blynk.server.api.websockets.handlers.WebSocketsGenericLoginHandler;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.common.AlreadyLoggedHandler;
import cc.blynk.server.handlers.common.HardwareNotLoggedHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareLoginHandler;
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
public class HardwareAndHttpAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HardwareAndHttpAPIServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("http.port"), holder.transportTypeHolder);

        final LetsEncryptHandler letsEncryptHandler = new LetsEncryptHandler(holder.sslContextHolder.contentHolder);

        final HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(holder, port);
        final HardwareChannelStateHandler hardwareChannelStateHandler =
                new HardwareChannelStateHandler(holder);
        final AlreadyLoggedHandler alreadyLoggedHandler = new AlreadyLoggedHandler();
        final int maxWebLength = holder.limits.webRequestMaxSize;
        final int hardTimeoutSecs = holder.limits.hardwareIdleTimeout;

        GlobalStats stats = holder.stats;
        WebSocketsGenericLoginHandler genericLoginHandler = new WebSocketsGenericLoginHandler(holder, port);

        //http API handlers
        ResetPasswordLogic resetPasswordLogic = new ResetPasswordLogic(holder);
        HttpAPILogic httpAPILogic = new HttpAPILogic(holder);
        NoMatchHandler noMatchHandler = new NoMatchHandler();

        BaseWebSocketUnificator baseWebSocketUnificator = new BaseWebSocketUnificator() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                FullHttpRequest req = (FullHttpRequest) msg;
                String uri = req.uri();

                log.debug("In http and websocket unificator handler.");
                if (uri.equals("/")) {
                    //for local server do redirect to admin page
                    try {
                        ctx.writeAndFlush(redirect(BLYNK_LANDING));
                    } finally {
                        req.release();
                    }
                    return;
                } else if (uri.startsWith(WEBSOCKET_PATH)) {
                    initWebSocketPipeline(ctx, WEBSOCKET_PATH);
                } else {
                    initHttpPipeline(ctx);
                }

                ctx.fireChannelRead(msg);
            }

            private void initHttpPipeline(ChannelHandlerContext ctx) {
                ctx.pipeline()
                        .addLast(letsEncryptHandler)
                        .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                        .addLast("HttpUrlMapper", new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                        .addLast("HttpStaticFile", new StaticFileHandler(holder.props, new StaticFile("/static"),
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
                ch.pipeline().addLast(
                        new BaseHttpAndBlynkUnificationHandler() {
                            @Override
                            public ChannelPipeline buildHttpPipeline(ChannelPipeline pipeline) {
                                log.trace("HTTP connection detected.", pipeline.channel());
                                return pipeline
                                        .addLast("HttpServerCodec", new HttpServerCodec())
                                        .addLast("HttpServerKeepAlive", new HttpServerKeepAliveHandler())
                                        .addLast("HttpObjectAggregator", new HttpObjectAggregator(maxWebLength, true))
                                        .addLast("HttpWebSocketUnificator", baseWebSocketUnificator);
                            }

                            @Override
                            public ChannelPipeline buildBlynkPipeline(ChannelPipeline pipeline) {
                                log.trace("Blynk protocol connection detected.", pipeline.channel());
                                return pipeline
                                        .addLast("H_IdleStateHandler",
                                                new IdleStateHandler(hardTimeoutSecs, hardTimeoutSecs, 0))
                                        .addLast("H_ChannelState", hardwareChannelStateHandler)
                                        .addLast("H_MessageDecoder", new MessageDecoder(holder.stats))
                                        .addLast("H_MessageEncoder", new MessageEncoder(holder.stats))
                                        .addLast("H_Login", hardwareLoginHandler)
                                        .addLast("H_NotLogged", new HardwareNotLoggedHandler())
                                        .addLast("H_AlreadyLogged", alreadyLoggedHandler);
                            }
                        }
                );
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTP API and WebSockets";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTP API and WebSockets server...");
        super.close();
    }

}
