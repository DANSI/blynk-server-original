package cc.blynk.server.servers.hardware;

import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.url.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.BaseHttpAndBlynkUnificationHandler;
import cc.blynk.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import cc.blynk.server.api.http.handlers.LetsEncryptHandler;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.handlers.common.AlreadyLoggedHandler;
import cc.blynk.server.handlers.common.HardwareNotLoggedHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.server.servers.BaseServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

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

        final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler =
                new HttpAndWebSocketUnificatorHandler(holder, port);
        final LetsEncryptHandler letsEncryptHandler = new LetsEncryptHandler(holder.sslContextHolder.contentHolder);

        final HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(holder, port);
        final HardwareChannelStateHandler hardwareChannelStateHandler =
                new HardwareChannelStateHandler(holder);
        final AlreadyLoggedHandler alreadyLoggedHandler = new AlreadyLoggedHandler();
        final int maxWebLength = holder.limits.webRequestMaxSize;
        final int hardTimeoutSecs = holder.limits.hardwareIdleTimeout;

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
                                        .addLast(letsEncryptHandler)
                                        .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                                        .addLast("HttpUrlMapper",
                                                new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                                        .addLast("HttpStaticFile",
                                                new StaticFileHandler(holder.props, new StaticFile("/static"),
                                                new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")))
                                        .addLast("HttpWebSocketUnificator", httpAndWebSocketUnificatorHandler);
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
