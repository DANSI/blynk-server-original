package cc.blynk.server.transport;

import cc.blynk.utils.properties.ServerProperties;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;

/**
 * Used in order to re-use EventLoopGroups, this is done for performance reasons.
 * To create less threads and minimize memory footprint (recommended way by netty devs)
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.04.15.
 */
public class TransportTypeHolder implements Closeable {

    private static final Logger log = LogManager.getLogger(TransportTypeHolder.class);

    public final EventLoopGroup bossGroup;
    public final EventLoopGroup workerGroup;
    public final Class<? extends ServerChannel> channelClass;

    public TransportTypeHolder(ServerProperties serverProperties) {
        this(serverProperties.getIntProperty("server.worker.threads", Runtime.getRuntime().availableProcessors() * 2));
    }

    private TransportTypeHolder(int workerThreads) {
        if (Epoll.isAvailable()) {
            log.info("Using native epoll transport.");
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup(workerThreads);
            channelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(workerThreads);
            channelClass = NioServerSocketChannel.class;
        }
    }

    @Override
    public void close() {
        System.out.println("Stopping Transport Holder...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

}
