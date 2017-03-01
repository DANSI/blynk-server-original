package cc.blynk.server.api.http;

import cc.blynk.core.http.rest.HandlerRegistry;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import cc.blynk.server.api.http.logic.HttpAPILogic;
import cc.blynk.server.core.BaseServer;
import cc.blynk.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
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
        super(holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        HandlerRegistry.register(new HttpAPILogic(holder));

        final SslContext sslCtx = SslUtil.initSslContext(
                holder.props.getProperty("https.cert", holder.props.getProperty("server.ssl.cert")),
                holder.props.getProperty("https.key", holder.props.getProperty("server.ssl.key")),
                holder.props.getProperty("https.key.pass", holder.props.getProperty("server.ssl.key.pass")),
                SslUtil.fetchSslProvider(holder.props));

        final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler = new HttpAndWebSocketUnificatorHandler(holder, port);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("HttpsSslContext", sslCtx.newHandler(ch.alloc()));
                pipeline.addLast("HttpsServerCodec", new HttpServerCodec());
                pipeline.addLast("HttpsObjectAggregator", new HttpObjectAggregator(65536, true));
                pipeline.addLast("HttpsWebSocketUnificator", httpAndWebSocketUnificatorHandler);
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTPS API and WebSockets";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTPS API and WebSockets server...");
        super.close();
    }

}
