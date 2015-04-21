package cc.blynk.server.core.administration;

import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.administration.AdminReplayingMessageDecoder;
import cc.blynk.server.handlers.administration.ExecutorHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringEncoder;

/**
* The Blynk Project.
* Created by Dmitriy Dumanskiy.
* Created on 11.03.15.
*/
final class AdminChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final StringEncoder ENCODER = new StringEncoder();

    private final SessionsHolder sessionsHolder;
    private final UserRegistry userRegistry;

    public AdminChannelInitializer(UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        this.userRegistry = userRegistry;
        this.sessionsHolder = sessionsHolder;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(ENCODER);

        pipeline.addLast(new AdminReplayingMessageDecoder());
        pipeline.addLast(new ExecutorHandler(userRegistry, sessionsHolder));


    }
}
