package cc.blynk.server.core.hardware;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.http.admin.HttpAdminHandler;
import cc.blynk.server.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
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
public class HttpsHardwareServer extends BaseServer {
    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsHardwareServer(Holder holder) {
        super(holder.props.getIntProperty("hardware.https.port"), holder.transportType);

        log.info("Enabling HTTPS for hardware.");
        SslContext sslCtx = SslUtil.initSslContext(holder.props);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new ChunkedWriteHandler());
                //look like not all hardwares can support that
                //pipeline.addLast(new HttpContentCompressor());
                pipeline.addLast(new HttpAdminHandler());
            }
        };

        log.info("HTTPS hardware server port {}.", port);
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
