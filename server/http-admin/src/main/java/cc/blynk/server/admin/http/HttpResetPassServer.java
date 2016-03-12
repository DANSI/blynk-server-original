package cc.blynk.server.admin.http;

import cc.blynk.server.Holder;
import cc.blynk.server.admin.http.handlers.ResetPassHandler;
import cc.blynk.server.admin.http.logic.ResetPasswordLogic;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpResetPassServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpResetPassServer(Holder holder) {
        super(holder.props.getIntProperty("reset.pass.http.port", 7444));

        HandlerRegistry.register("", new ResetPasswordLogic(holder.props, holder.userDao, holder.mailWrapper));

        log.info("Enabling HTTP reset pass server.");

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    new HttpServerCodec(),
                    new HttpObjectAggregator(65536),
                    new ChunkedWriteHandler(),
                    new ResetPassHandler(holder.userDao, holder.sessionDao, holder.stats)
                );
            }
        };

        log.info("HTTP reset pass port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTP Reset Pass";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTP Reset Pass server...");
        super.close();
    }

}
