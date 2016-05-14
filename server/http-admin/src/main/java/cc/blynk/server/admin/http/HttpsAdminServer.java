package cc.blynk.server.admin.http;

import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.handlers.IpFilterHandler;
import cc.blynk.server.admin.http.logic.admin.ConfigsLogic;
import cc.blynk.server.admin.http.logic.admin.StatsLogic;
import cc.blynk.server.admin.http.logic.admin.UsersLogic;
import cc.blynk.server.core.BaseHttpHandler;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.http.logic.StaticFileHandler;
import cc.blynk.server.handlers.http.logic.UrlMapperHandler;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import cc.blynk.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.DomainNameMapping;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpsAdminServer extends BaseServer {
    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsAdminServer(Holder holder, boolean isUnpacked) {
        super(holder.props.getIntProperty("administration.https.port", 7443));

        final String adminRootPath = holder.props.getProperty("admin.rootPath", "/admin");

        HandlerRegistry.register(adminRootPath, new UsersLogic(holder.userDao, holder.sessionDao, holder.fileManager, holder.profileSaverWorker));
        HandlerRegistry.register(adminRootPath, new StatsLogic(holder.userDao, holder.sessionDao, holder.stats));
        HandlerRegistry.register(adminRootPath, new ConfigsLogic(holder.props, holder.blockingIOProcessor));

        final DomainNameMapping<SslContext> mappings = SslUtil.getDomainMappings(holder.props);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    new IpFilterHandler(holder.props.getCommaSeparatedValueAsArray("allowed.administrator.ips")),
                    new SniHandler(mappings),
                    new HttpServerCodec(),
                    new HttpObjectAggregator(65536),
                    new ChunkedWriteHandler(),
                    new UrlMapperHandler(adminRootPath, "/admin/static/admin.html"),
                    new UrlMapperHandler("/favicon.ico", "/admin/static/favicon.ico"),
                    new StaticFileHandler(isUnpacked, "/admin/static"),
                    new BaseHttpHandler(holder.userDao, holder.sessionDao, holder.stats)
                );
            }
        };

        log.info("HTTPS admin UI port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTPS Admin UI";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTPS Admin UI server...");
        super.close();
    }

}
