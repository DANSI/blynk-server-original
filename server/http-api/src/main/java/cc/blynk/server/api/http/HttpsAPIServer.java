package cc.blynk.server.api.http;

import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpHandler;
import cc.blynk.server.api.http.logic.HttpAPILogic;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import cc.blynk.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpsAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsAPIServer(Holder holder) {
        super(holder.props.getIntProperty("https.port"), holder.transportType);

        HandlerRegistry.register(new HttpAPILogic(holder.userDao, holder.sessionDao, holder.blockingIOProcessor, holder.stats));

        log.info("Enabling HTTPS API.");

        SslContext sslCtx = SslUtil.initSslContext(holder.props);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        sslCtx.newHandler(ch.alloc()),
                        new HttpServerCodec(),
                        new HttpObjectAggregator(65536),
                        new HttpHandler(holder.userDao, holder.sessionDao)
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
    public void stop() {
        System.out.println("Shutting down HTTPS API server...");
        super.stop();
    }

}
