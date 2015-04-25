package cc.blynk.server.core.administration;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AdminServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AdminServer(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                       TransportTypeHolder transportType) {
        super(props.getIntProperty("server.admin.port"), transportType);
        this.channelInitializer = new AdminChannelInitializer(userRegistry, sessionsHolder);
        log.info("Administration server port {}.", port);
    }

    @Override
    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return null;
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
