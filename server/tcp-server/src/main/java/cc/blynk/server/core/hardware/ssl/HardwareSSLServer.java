package cc.blynk.server.core.hardware.ssl;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.server.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.server.utils.SslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareSSLServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HardwareSSLServer(Holder holder) {
        super(holder.props.getIntProperty("hardware.ssl.port"), holder.transportType);

        final HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(holder.props, holder.userDao, holder.sessionDao, holder.reportingDao, holder.blockingIOProcessor);
        final HardwareChannelStateHandler hardwareChannelStateHandler = new HardwareChannelStateHandler(holder.sessionDao, holder.blockingIOProcessor);
        final UserNotLoggedHandler userNotLoggedHandler = new UserNotLoggedHandler();

        int hardTimeoutSecs = holder.props.getIntProperty("hard.socket.idle.timeout", 0);

        log.info("Enabling SSL for hardware.");
        SslContext sslCtx = SslUtil.initSslContext(holder.props);

        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (hardTimeoutSecs > 0) {
                    pipeline.addLast(new ReadTimeoutHandler(hardTimeoutSecs));
                }
                pipeline.addLast(sslCtx.newHandler(ch.alloc()));

                pipeline.addLast(hardwareChannelStateHandler);
                pipeline.addLast(new MessageDecoder(holder.stats));
                pipeline.addLast(new MessageEncoder());

                pipeline.addLast(hardwareLoginHandler);
                pipeline.addLast(userNotLoggedHandler);
            }
        };

        log.info("SSL hardware port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "hardware ssl";
    }

    @Override
    public void stop() {
        log.info("Shutting down default server...");
        super.stop();
    }

}
