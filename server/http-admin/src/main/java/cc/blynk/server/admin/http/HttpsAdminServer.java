package cc.blynk.server.admin.http;

import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.handlers.AdminHandler;
import cc.blynk.server.admin.http.handlers.IpFilterHandler;
import cc.blynk.server.admin.http.logic.StatsLogic;
import cc.blynk.server.admin.http.logic.UsersLogic;
import cc.blynk.server.core.BaseServer;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpsAdminServer extends BaseServer {
    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsAdminServer(Holder holder) {
        super(holder.props.getIntProperty("administration.https.port", 7443));

        final String rootPath = holder.props.getProperty("admin.rootPath", "/admin");

        HandlerRegistry.register(rootPath, new UsersLogic(holder.userDao, holder.sessionDao, holder.fileManager, holder.profileSaverWorker));
        HandlerRegistry.register(rootPath, new StatsLogic(holder.userDao, holder.sessionDao, holder.stats));

        final String[] allowedIPsArray = holder.props.getCommaSeparatedList("allowed.administrator.ips");
        final Set<String> allowedIPs;

        if (allowedIPsArray != null && allowedIPsArray.length > 0 &&
                allowedIPsArray[0] != null && !"".equals(allowedIPsArray[0])) {
            allowedIPs = new HashSet<>(Arrays.asList(allowedIPsArray));
        } else {
            allowedIPs = Collections.emptySet();
        }

        final DomainNameMapping<SslContext> mappings = SslUtil.getDomainMappings(holder.props);
        final IpFilterHandler ipFilterHandler = new IpFilterHandler(allowedIPs);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    ipFilterHandler,
                    new SniHandler(mappings),
                    new HttpServerCodec(),
                    new HttpObjectAggregator(65536),
                    new ChunkedWriteHandler(),
                    new AdminHandler(holder.userDao, holder.sessionDao, holder.stats, rootPath)
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
