package cc.blynk.server.api.http;

import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.UrlMapperHandler;
import cc.blynk.core.http.rest.HandlerRegistry;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpHandler;
import cc.blynk.server.api.http.logic.HttpAPILogic;
import cc.blynk.server.api.http.logic.business.*;
import cc.blynk.server.core.BaseServer;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpsAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsAPIServer(Holder holder, boolean isUnpacked) {
        super(holder.props.getIntProperty("https.port"));

        HandlerRegistry.register(new HttpAPILogic(holder));
        HandlerRegistry.register(new HttpBusinessAPILogic(holder));

        final String businessRootPath = holder.props.getProperty("business.rootPath", "/business");

        final SessionHolder sessionHolder = new SessionHolder();

        HandlerRegistry.register(businessRootPath, new BusinessLogic(holder.userDao, holder.sessionDao, holder.fileManager));
        HandlerRegistry.register(businessRootPath, new BusinessAuthLogic(holder.userDao, holder.sessionDao, holder.fileManager, sessionHolder));

        final SslContext sslCtx = SslUtil.initSslContext(
                holder.props.getProperty("https.cert", holder.props.getProperty("server.ssl.cert")),
                holder.props.getProperty("https.key", holder.props.getProperty("server.ssl.key")),
                holder.props.getProperty("https.key.pass", holder.props.getProperty("server.ssl.key.pass")),
                SslUtil.fetchSslProvider(holder.props));

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        sslCtx.newHandler(ch.alloc()),
                        new HttpServerCodec(),
                        new HttpObjectAggregator(65536, true),
                        new ChunkedWriteHandler(),

                        new AuthCookieHandler(businessRootPath, sessionHolder),
                        new UrlMapperHandler(businessRootPath, "/static/business/business.html"),
                        new UrlMapperHandler("/favicon.ico", "/static/favicon.ico"),
                        new StaticFileHandler(isUnpacked, new StaticFile("/static", false), new StaticFile(FileUtils.CSV_DIR, true, false)),
                        new AuthHttpHandler(holder.userDao, holder.sessionDao, holder.stats),
                        new HttpHandler(holder.userDao, holder.sessionDao, holder.stats)
                );
            }
        };

        log.info("HTTPS API port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTPS API";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTPS API server...");
        super.close();
    }

}
