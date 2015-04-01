package cc.blynk.server.core;

import cc.blynk.server.core.hardware.HardwareServer;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
    private final int workerThreads;
    private final boolean enableNativeEpoll;

    private Channel channel;

    protected BaseServer(int port, int workerThreads, boolean enableNativeEpoll) {
        this.port = port;
        this.workerThreads = workerThreads;
        this.enableNativeEpoll = enableNativeEpoll;
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerChannel> channelClass;

        if (enableNativeEpoll) {
            log.warn("Native epoll transport enabled.");
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup(workerThreads);
            channelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(workerThreads);
            channelClass = NioServerSocketChannel.class;
        }

        buildServerAndRun(bossGroup, workerGroup, channelClass);
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
