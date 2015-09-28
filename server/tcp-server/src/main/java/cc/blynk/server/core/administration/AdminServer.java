package cc.blynk.server.core.administration;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AdminServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AdminServer(Holder holder) {
        super(holder.props.getIntProperty("server.admin.port"), holder.transportType);
        this.channelInitializer = new AdminChannelInitializer(holder.userRegistry, holder.sessionsHolder);
        log.info("Administration server port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    public void stop() {
        log.info("Shutting down admin server...");
        super.stop();
    }

}
