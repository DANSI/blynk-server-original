package cc.blynk.server.core.administration.handlers;

import cc.blynk.server.core.administration.ByteClassLoader;
import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ExecutorHandler extends SimpleChannelInboundHandler<byte[]> {

    private final ByteClassLoader byteClassLoader;
    private final SessionsHolder sessionsHolder;
    private final UserRegistry userRegistry;

    public ExecutorHandler(UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        this.sessionsHolder = sessionsHolder;
        this.userRegistry = userRegistry;
        this.byteClassLoader = new ByteClassLoader();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        Executable executable = byteClassLoader.defineClass(msg);

        String result = executable.execute(userRegistry, sessionsHolder);

        ctx.writeAndFlush(result);
    }

}
