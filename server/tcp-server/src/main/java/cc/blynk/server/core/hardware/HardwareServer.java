package cc.blynk.server.core.hardware;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.common.UserNotLoggerHandler;
import cc.blynk.server.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareLoginHandler;
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

    public HardwareServer(Holder holder) {
        super(holder.props.getIntProperty("hardware.default.port"), holder.transportType);

        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(holder.props, holder.userRegistry, holder.sessionsHolder, holder.storageDao, holder.notificationsProcessor);
        HardwareChannelStateHandler hardwareChannelStateHandler = new HardwareChannelStateHandler(holder.sessionsHolder, holder.notificationsProcessor);

        int hardTimeoutSecs = holder.props.getIntProperty("hard.socket.idle.timeout", 0);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                //non-sharable handlers
                if (hardTimeoutSecs > 0) {
                    pipeline.addLast(new ReadTimeoutHandler(hardTimeoutSecs));
                }
                pipeline.addLast(hardwareChannelStateHandler);
                pipeline.addLast(new MessageDecoder(holder.stats));
                pipeline.addLast(new MessageEncoder());

                //sharable business logic handlers
                pipeline.addLast(hardwareLoginHandler);
                pipeline.addLast(new UserNotLoggerHandler());
            }
        };

        log.debug("hard.socket.idle.timeout = {}", hardTimeoutSecs);

        log.info("Plain tcp/ip hardware server port {}.", port);
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
