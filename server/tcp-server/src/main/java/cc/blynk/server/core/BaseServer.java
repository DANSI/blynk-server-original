package cc.blynk.server.core;

import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.hardware.HardwareServer;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 3/10/2015.
 */
public abstract class BaseServer implements Runnable {

    protected static final Logger log = LogManager.getLogger(HardwareServer.class);
    protected final int port;
    private final TransportTypeHolder transportTypeHolder;

    private Channel channel;

    protected BaseServer(int port, TransportTypeHolder transportTypeHolder) {
        this.port = port;
        this.transportTypeHolder = transportTypeHolder;
    }

    @Override
    public void run() {
        if (transportTypeHolder.epollEnabled) {
            log.warn("Native epoll transport enabled.");
        }
        buildServerAndRun(
                transportTypeHolder.bossGroup,
                transportTypeHolder.workerGroup,
                transportTypeHolder.channelClass
        );
    }

    private void buildServerAndRun(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                             Class<? extends ServerChannel> channelClass) {

        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(bossGroup, workerGroup)
                    .channel(channelClass)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(getChannelInitializer());

            ChannelFuture channelFuture = b.bind(port).sync();

            this.channel = channelFuture.channel();
            this.channel.closeFuture().sync();
        } catch (Exception e) {
            log.error(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public abstract BaseSimpleChannelInboundHandler[] getBaseHandlers();

    protected abstract ChannelInitializer<SocketChannel> getChannelInitializer();

    public void stop() {
        if (channel != null) {
            channel.close().awaitUninterruptibly();
        }
    }
}
