package cc.blynk.server.api.http;

import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import cc.blynk.server.core.BaseServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;
    public static final String WEBSOCKET_PATH = "/websocket";

    public HttpAPIServer(Holder holder) {
        super(holder.props.getProperty("listen.address"), holder.props.getIntProperty("http.port"), holder.transportTypeHolder);

        String adminRootPath = holder.props.getProperty("admin.rootPath", "/admin");

        HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler = new HttpAndWebSocketUnificatorHandler(holder, port, adminRootPath);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("HttpServerCodec", new HttpServerCodec());
                pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(65536, true));
                pipeline.addLast("HttpWebSocketUnificator", httpAndWebSocketUnificatorHandler);
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
