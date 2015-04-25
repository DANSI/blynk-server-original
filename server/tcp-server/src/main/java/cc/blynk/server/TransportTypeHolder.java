package cc.blynk.server;

import cc.blynk.common.utils.ServerProperties;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Used in order to re-user EventLoopGroups, this is done for performance reasons.
 * To create less threads and minimize memory footprint.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.04.15.
 */
public class TransportTypeHolder {

    public final EventLoopGroup bossGroup;
    public final EventLoopGroup workerGroup;
    public final Class<? extends ServerChannel> channelClass;
    public final boolean epollEnabled;

    public TransportTypeHolder(ServerProperties serverProperties) {
        this(serverProperties.getBoolProperty("enable.native.epoll.transport"),
                serverProperties.getIntProperty("server.worker.threads", Runtime.getRuntime().availableProcessors() * 2));
    }

    private TransportTypeHolder(boolean enableNativeEpoll, int workerThreads) {
        epollEnabled = enableNativeEpoll;
        if (enableNativeEpoll) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup(workerThreads);
            channelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(workerThreads);
            channelClass = NioServerSocketChannel.class;
        }
    }
}
