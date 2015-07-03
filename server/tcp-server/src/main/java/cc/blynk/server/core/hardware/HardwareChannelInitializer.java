package cc.blynk.server.core.hardware;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.handlers.common.ClientChannelStateHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
* The Blynk Project.
* Created by Dmitriy Dumanskiy.
* Created on 11.03.15.
*/
public final class HardwareChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final SessionsHolder sessionsHolder;
    private final GlobalStats stats;
    private final HardwareHandlersHolder handlersHolder;
    private final int hardTimeoutSecs;
    private final SslContext sslCtx;

    public HardwareChannelInitializer(SessionsHolder sessionsHolder, GlobalStats stats, HardwareHandlersHolder handlersHolder, int hardTimeoutSecs, SslContext sslCtx) {
        this.sessionsHolder = sessionsHolder;
        this.stats = stats;
        this.handlersHolder = handlersHolder;
        this.hardTimeoutSecs = hardTimeoutSecs;
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        //non-sharable handlers
        pipeline.addLast(new ReadTimeoutHandler(hardTimeoutSecs));

        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }

        pipeline.addLast(new ClientChannelStateHandler(sessionsHolder, handlersHolder.getNotificationsProcessor()));
        pipeline.addLast(new MessageDecoder(stats));
        pipeline.addLast(new MessageEncoder());

        //sharable business logic handlers initialized previously
        for (ChannelHandler handler : handlersHolder.getAllHandlers()) {
            pipeline.addLast(handler);
        }
    }
}
