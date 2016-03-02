package cc.blynk.server.hardware.ssl;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.utils.SslUtil;
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
        super(holder.props.getIntProperty("hardware.ssl.port"));

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
                pipeline.addLast(
                        sslCtx.newHandler(ch.alloc()),
                        hardwareChannelStateHandler,
                        new MessageDecoder(holder.stats),
                        new MessageEncoder(holder.stats),
                        hardwareLoginHandler,
                        userNotLoggedHandler
                );
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
    public void close() {
        System.out.println("Shutting down ssl hardware server...");
        super.close();
    }

}
