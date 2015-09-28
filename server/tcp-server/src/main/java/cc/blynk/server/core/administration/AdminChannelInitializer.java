package cc.blynk.server.core.administration;

import cc.blynk.server.core.administration.handlers.AdminReplayingMessageDecoder;
import cc.blynk.server.core.administration.handlers.ExecutorHandler;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
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

    private final SessionDao sessionDao;
    private final UserDao userDao;

    public AdminChannelInitializer(UserDao userDao, SessionDao sessionDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(ENCODER);

        pipeline.addLast(new AdminReplayingMessageDecoder());
        pipeline.addLast(new ExecutorHandler(userDao, sessionDao));


    }
}
