package cc.blynk.server.api.http;

import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.UrlReWriterHandler;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import cc.blynk.server.api.http.handlers.LetsEncryptHandler;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.dao.CSVGenerator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;
    public static final String WEBSOCKET_PATH = "/websocket";

    public HttpAPIServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("http.port"), holder.transportTypeHolder);

        String adminRootPath = holder.props.getProperty("admin.rootPath", "/admin");

        final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler =
                new HttpAndWebSocketUnificatorHandler(holder, port, adminRootPath);
        final LetsEncryptHandler letsEncryptHandler = new LetsEncryptHandler(holder.sslContextHolder.contentHolder);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                .addLast("HttpServerCodec", new HttpServerCodec())
                .addLast("HttpServerKeepAlive", new HttpServerKeepAliveHandler())
                .addLast("HttpObjectAggregator", new HttpObjectAggregator(holder.limits.webRequestMaxSize, true))
                .addLast(letsEncryptHandler)
                .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                .addLast("HttpUrlMapper", new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                .addLast("HttpStaticFile", new StaticFileHandler(holder.props, new StaticFile("/static"),
                        new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")))
                .addLast("HttpWebSocketUnificator", httpAndWebSocketUnificatorHandler);
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
