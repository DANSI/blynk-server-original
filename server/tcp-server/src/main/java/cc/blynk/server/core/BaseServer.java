package cc.blynk.server.core;

import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.hardware.HardwareServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base server abstraction. Class responsible for Netty EventLoops creation.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 3/10/2015.
 */
public abstract class BaseServer implements Runnable {

    protected static final Logger log = LogManager.getLogger(HardwareServer.class);
    protected final int port;
    private final TransportTypeHolder transportTypeHolder;
    public volatile boolean isRunning;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    protected BaseServer(int port, TransportTypeHolder transportTypeHolder) {
        this.port = port;
        this.transportTypeHolder = transportTypeHolder;
        this.isRunning = true;
    }

    @Override
    public void run() {
        if (transportTypeHolder.epollEnabled) {
            log.warn("Native epoll transport for {} server enabled.", getServerName());
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

            b.bind(port).sync();

            this.bossGroup = bossGroup;
            this.workerGroup = workerGroup;
        } catch (Exception e) {
            log.error("Error initializing {}", getServerName(), e);
            this.isRunning = false;
        }
    }

    protected abstract ChannelInitializer<SocketChannel> getChannelInitializer();

    protected abstract String getServerName();

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
