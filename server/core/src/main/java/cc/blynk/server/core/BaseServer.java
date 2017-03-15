package cc.blynk.server.core;

import cc.blynk.server.transport.TransportTypeHolder;
import cc.blynk.utils.BlynkByteBufUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.net.InetSocketAddress;

/**
 * Base server abstraction. Class responsible for Netty EventLoops starting amd port listening.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 3/10/2015.
 */
public abstract class BaseServer implements Closeable {

    protected static final Logger log = LogManager.getLogger(BaseServer.class);

    private final String listenAddress;
    protected final int port;
    private final TransportTypeHolder transportTypeHolder;

    private ChannelFuture cf;

    protected BaseServer(String listenAddress, int port, TransportTypeHolder transportTypeHolder) {
        this.listenAddress = listenAddress;
        this.port = port;
        this.transportTypeHolder = transportTypeHolder;
    }

    public BaseServer start() throws Exception {
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
                    .childOption(ChannelOption.ALLOCATOR, BlynkByteBufUtil.ALLOCATOR)
                    .option(ChannelOption.ALLOCATOR, BlynkByteBufUtil.ALLOCATOR)
                    .childHandler(getChannelInitializer());

            InetSocketAddress listenTo = listenAddress == null ? new InetSocketAddress(port) : new InetSocketAddress(listenAddress, port);
            this.cf = b.bind(listenTo).sync();
        } catch (Exception e) {
            log.error("Error initializing {}, port {}", getServerName(), port, e);
            throw e;
        }

        log.info("{} server listening at {} port.", getServerName(), port);
    }

    protected abstract ChannelInitializer<SocketChannel> getChannelInitializer();

    protected abstract String getServerName();

    @Override
    public void close() {
        cf.channel().close();
    }
}
