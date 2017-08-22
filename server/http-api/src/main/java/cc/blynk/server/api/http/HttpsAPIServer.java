package cc.blynk.server.api.http;

import cc.blynk.core.http.handlers.*;
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

import static cc.blynk.server.api.http.HttpAPIServer.HTTP_REQUEST_SIZE_MAX;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpsAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsAPIServer(Holder holder, boolean isUnpacked) {
        super(holder.props.getProperty("listen.address"), holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        String adminRootPath = holder.props.getProperty("admin.rootPath", "/admin");

        final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler =
                new HttpAndWebSocketUnificatorHandler(holder, port, adminRootPath, isUnpacked);
        final LetsEncryptHandler letsEncryptHandler = new LetsEncryptHandler(holder.sslContextHolder.contentHolder);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                .addLast("HttpsSslContext", holder.sslContextHolder.sslCtx.newHandler(ch.alloc()))
                .addLast("HttpsServerCodec", new HttpServerCodec())
                .addLast("HttpsServerKeepAlive", new HttpServerKeepAliveHandler())
                .addLast("HttpsObjectAggregator", new HttpObjectAggregator(HTTP_REQUEST_SIZE_MAX, true))
                .addLast(letsEncryptHandler)
                .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                .addLast("HttpUrlMapper", new UrlReWriterHandler("/favicon.ico", "/static/favicon.ico"))
                .addLast("HttpStaticFile", new StaticFileHandler(isUnpacked, new StaticFile("/static"),
                                           new StaticFileEdsWith(CSVGenerator.CSV_DIR, ".csv.gz")))
                .addLast("HttpsWebSocketUnificator", httpAndWebSocketUnificatorHandler)
                .addLast(new OTAHandler(holder, adminRootPath + "/ota/start", "/static/ota", isUnpacked));
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
