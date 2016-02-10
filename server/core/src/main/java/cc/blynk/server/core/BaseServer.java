package cc.blynk.server.core;

import cc.blynk.server.TransportTypeHolder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
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

    protected static final Logger log = LogManager.getLogger(BaseServer.class);

    protected final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture cf;

    protected BaseServer(int port) {
        this.port = port;
    }

    public BaseServer start(TransportTypeHolder transportTypeHolder) throws Exception {
        if (transportTypeHolder.epollEnabled) {
            log.warn("Native epoll transport for {} server enabled.", getServerName());
        }
        buildServerAndRun(
                transportTypeHolder.bossGroup,
                transportTypeHolder.workerGroup,
                transportTypeHolder.channelClass
        );

        return this;
    }

    private void buildServerAndRun(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                             Class<? extends ServerChannel> channelClass) throws Exception {

        ServerBootstrap b = new ServerBootstrap();
        try {
            b.group(bossGroup, workerGroup)
                    .channel(channelClass)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(getChannelInitializer());

            this.cf = b.bind(port).sync();

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
        cf.channel().close().awaitUninterruptibly();

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
