package cc.blynk.server.core.hardware;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.hardware.http.HttpHardwareHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpHardwareServer extends BaseServer {
    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpHardwareServer(Holder holder) {
        super(holder.props.getIntProperty("hardware.http.port"), holder.transportType);

        log.info("Enabling HTTP for hardware.");

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpServerCodec());
                //look like not all hardwares can support that
                //pipeline.addLast(new HttpContentCompressor());
                pipeline.addLast(new HttpHardwareHandler(holder.userDao));
            }
        };

        log.info("HTTP hardware server port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTP Hardware";
    }

    @Override
    public void stop() {
        System.out.println("Shutting down http hardware server...");
        super.stop();
    }

}
