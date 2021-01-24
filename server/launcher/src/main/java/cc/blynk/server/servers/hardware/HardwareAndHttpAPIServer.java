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
import cc.blynk.server.api.http.logic.ResetPasswordHttpLogic;
import cc.blynk.server.api.websockets.handlers.WSHandler;
import cc.blynk.server.api.websockets.handlers.WSWrapperEncoder;
import cc.blynk.server.common.handlers.AlreadyLoggedHandler;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
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

        LetsEncryptHandler letsEncryptHandler = new LetsEncryptHandler(holder.sslContextHolder.contentHolder);
        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(holder, port);
        HardwareChannelStateHandler hardwareChannelStateHandler = new HardwareChannelStateHandler(holder);
        AlreadyLoggedHandler alreadyLoggedHandler = new AlreadyLoggedHandler();
        int maxWebLength = holder.limits.webRequestMaxSize;
        int hardTimeoutSecs = NumberUtil.calcHeartbeatTimeout(holder.limits.hardwareIdleTimeout);

        GlobalStats stats = holder.stats;

        //http API handlers
        ResetPasswordHttpLogic resetPasswordLogic = new ResetPasswordHttpLogic(holder);
        HttpAPILogic httpAPILogic = new HttpAPILogic(holder);
        NoMatchHandler noMatchHandler = new NoMatchHandler();
        BaseWebSocketUnificator baseWebSocketUnificator = new BaseWebSocketUnificator() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                var req = (FullHttpRequest) msg;
                var uri = req.uri();

                log.trace("In http and websocket unificator handler.");
                if (uri.equals("/")) {
                    //for local server do redirect to admin page
                    try {
                        ctx.writeAndFlush(redirect(BLYNK_LANDING));
                    } finally {
                        req.release();
                    }
                    return;
                } else if (uri.startsWith(WEBSOCKET_PATH)) {
                    initWebSocketPipeline(ctx, WEBSOCKETS_PATH);
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
                                        new StaticFileEdsWith(FileUtils.CSV_DIR, ".gz"),
                                        new StaticFileEdsWith(FileUtils.CSV_DIR, ".zip")))
                        .addLast(resetPasswordLogic)
                        .addLast(httpAPILogic)
                        .addLast(noMatchHandler)
                        .remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized http pipeline. {}", ctx.pipeline().names());
                }
            }

            private void initWebSocketPipeline(ChannelHandlerContext ctx, String websocketPath) {
                var pipeline = ctx.pipeline();

                //websockets specific handlers
                pipeline.addFirst("WSIdleStateHandler", new IdleStateHandler(hardTimeoutSecs, 0, 0))
                        .addLast("WSChannelState", hardwareChannelStateHandler)
                        .addLast("WSWebSocketServerProtocolHandler",
                        new WebSocketServerProtocolHandler(websocketPath, true))
                        .addLast("WSWebSocket", new WSHandler(stats))
                        .addLast("WSMessageDecoder", new MessageDecoder(stats, holder.limits))
                        .addLast("WSSocketWrapper", new WSWrapperEncoder())
                        .addLast("WSMessageEncoder", new MessageEncoder(stats))
                        .addLast("WSLogin", hardwareLoginHandler)
                        .addLast("WSNotLogged", alreadyLoggedHandler)
                        .remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized hardware websocket pipeline. {}", ctx.pipeline().names());
                }
            }
        };

        channelInitializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
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
                            //for hardware port we always expecting hardware and never app
                            public ChannelPipeline buildAppPipeline(ChannelPipeline pipeline) {
                                return buildHardwarePipeline(pipeline);
                            }

                            @Override
                            public ChannelPipeline buildHardwarePipeline(ChannelPipeline pipeline) {
                                log.trace("Blynk hardware plain protocol connection detected.", pipeline.channel());
                                return pipeline
                                        .addFirst("H_IdleStateHandler",
                                                new IdleStateHandler(hardTimeoutSecs, 0, 0))
                                        .addLast("H_ChannelState", hardwareChannelStateHandler)
                                        .addLast("H_MessageDecoder", new MessageDecoder(holder.stats, holder.limits))
                                        .addLast("H_MessageEncoder", new MessageEncoder(holder.stats))
                                        .addLast("H_Login", hardwareLoginHandler)
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
    public ChannelFuture close() {
        System.out.println("Shutting down HTTP API and WebSockets server...");
        return super.close();
    }

}
