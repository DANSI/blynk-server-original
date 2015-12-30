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
public abstract class BaseServer {

    protected static final Logger log = LogManager.getLogger(HardwareServer.class);
    protected final int port;
    private final TransportTypeHolder transportTypeHolder;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    protected BaseServer(int port, TransportTypeHolder transportTypeHolder) {
        this.port = port;
        this.transportTypeHolder = transportTypeHolder;
    }

    public void start() throws Exception {
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
                             Class<? extends ServerChannel> channelClass) throws Exception {

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
            log.error("Error initializing {}, port {}", getServerName(), port, e);
            throw e;
        }
    }

    protected abstract ChannelInitializer<SocketChannel> getChannelInitializer();

    protected abstract String getServerName();

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
