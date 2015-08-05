package cc.blynk.server.core.hardware;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.common.ClientChannelStateHandler;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;
    private final HardwareHandler hardwareHandler;

    public HardwareServer(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                          GlobalStats stats, NotificationsProcessor notificationsProcessor, TransportTypeHolder transportType, StorageDao storageDao) {
        super(props.getIntProperty("hardware.default.port"), transportType);

        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(userRegistry, sessionsHolder);
        this.hardwareHandler = new HardwareHandler(props, sessionsHolder, storageDao, notificationsProcessor);

        int hardTimeoutSecs = props.getIntProperty("hard.socket.idle.timeout", 15);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //non-sharable handlers
                pipeline.addLast(new ReadTimeoutHandler(hardTimeoutSecs));
                pipeline.addLast(new ClientChannelStateHandler(sessionsHolder, notificationsProcessor));
                pipeline.addLast(new MessageDecoder(stats));
                pipeline.addLast(new MessageEncoder());

                //sharable business logic handlers
                pipeline.addLast(hardwareLoginHandler);
                pipeline.addLast(hardwareHandler);
            }
        };

        log.debug("hard.socket.idle.timeout = {}", hardTimeoutSecs);

        log.info("Plain tcp/ip hardware server port {}.", port);
    }

    @Override
    public BaseSimpleChannelInboundHandler getBaseHandler() {
        return hardwareHandler;
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    public void stop() {
        log.info("Shutting down default server...");
        super.stop();
    }

}
