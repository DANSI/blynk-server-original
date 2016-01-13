package cc.blynk.server.websocket;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.websocket.handlers.WebSocketHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13/01/2016.
 */
public class WebSocketServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public WebSocketServer(Holder holder) {
        super(holder.props.getIntProperty("tcp.web-socket.port"), holder.transportType);

        log.info("Enabling Web Sockets.");

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new WebSocketHandler());
            }
        };

        log.info("Web Sockets port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "Web Sockets";
    }

    @Override
    public void stop() {
        System.out.println("Shutting down Web Sockets server...");
        super.stop();
    }

}
