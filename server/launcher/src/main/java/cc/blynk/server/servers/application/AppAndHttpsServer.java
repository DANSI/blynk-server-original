package cc.blynk.server.servers.application;

import cc.blynk.core.http.handlers.OTAHandler;
import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.url.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.BaseHttpAndBlynkUnificationHandler;
import cc.blynk.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.GetServerHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.core.dao.CSVGenerator;
import cc.blynk.server.core.protocol.handlers.decoders.AppMessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.AppMessageEncoder;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
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
public class AppAndHttpsServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AppAndHttpsServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        final AppChannelStateHandler appChannelStateHandler = new AppChannelStateHandler(holder.sessionDao);
        final RegisterHandler registerHandler = new RegisterHandler(holder);
        final AppLoginHandler appLoginHandler = new AppLoginHandler(holder);
        final AppShareLoginHandler appShareLoginHandler = new AppShareLoginHandler(holder);
        final UserNotLoggedHandler userNotLoggedHandler = new UserNotLoggedHandler();
        final GetServerHandler getServerHandler = new GetServerHandler(holder);

        final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler =
                new HttpAndWebSocketUnificatorHandler(holder, port);

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
                                .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                                .addLast("HttpUrlMapper",
                                        new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                                .addLast("HttpStaticFile",
                                        new StaticFileHandler(holder.props, new StaticFile("/static"),
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
