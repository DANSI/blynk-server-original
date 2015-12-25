package cc.blynk.server.core.admin;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.http.admin.AdminHandler;
import cc.blynk.server.handlers.http.admin.IpFilterHandler;
import cc.blynk.server.handlers.http.admin.handlers.StatsHandler;
import cc.blynk.server.handlers.http.admin.handlers.UsersHandler;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import cc.blynk.server.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.Arrays;
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
        super(holder.props.getIntProperty("https.port"), holder.transportType);

        HandlerRegistry.register(new UsersHandler(holder.userDao, holder.sessionDao, holder.fileManager));
        HandlerRegistry.register(new StatsHandler(holder.userDao, holder.sessionDao, holder.stats));

        final String[] allowedIPsArray = holder.props.getCommaSeparatedList("allowed.administrator.ips");
        final Set<String> allowedIPs;

        if (allowedIPsArray != null && allowedIPsArray.length > 0 &&
                allowedIPsArray[0] != null && !"".equals(allowedIPsArray[0])) {
            allowedIPs = new HashSet<>(Arrays.asList(allowedIPsArray));
        } else {
            allowedIPs = null;
        }

        log.info("Enabling HTTPS for admin UI.");
        SslContext sslCtx = SslUtil.initSslContext(holder.props);
        final IpFilterHandler ipFilterHandler = new IpFilterHandler(allowedIPs);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(ipFilterHandler);
                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new ChunkedWriteHandler());
                pipeline.addLast(new AdminHandler());
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
        return "HTTPS Hardware";
    }

    @Override
    public void stop() {
        System.out.println("Shutting down https hardware server...");
        super.stop();
    }

}
