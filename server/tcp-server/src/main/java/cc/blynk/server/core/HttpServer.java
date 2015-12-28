package cc.blynk.server.core;

import cc.blynk.server.Holder;
import cc.blynk.server.handlers.app.AppHttpHandler;
import cc.blynk.server.handlers.http.BaseHttpAPIHandler;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpServer(Holder holder) {
        super(holder.props.getIntProperty("http.port"), holder.transportType);

        HandlerRegistry.register(new AppHttpHandler(holder.userDao));

        log.info("Enabling HTTP for app.");

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new ChunkedWriteHandler());
                pipeline.addLast(new BaseHttpAPIHandler());
            }
        };

        log.info("HTTP for app port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTP app";
    }

    @Override
    public void stop() {
        System.out.println("Shutting down http app server...");
        super.stop();
    }

}
