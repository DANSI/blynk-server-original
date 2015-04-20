package cc.blynk.server.handlers.administration;

import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.utils.ByteClassLoaderUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ExecutorHandler extends SimpleChannelInboundHandler<byte[]> {

    private final ByteClassLoaderUtil byteClassLoaderUtil;
    private final SessionsHolder sessionsHolder;
    private final UserRegistry userRegistry;

    public ExecutorHandler(UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        this.sessionsHolder = sessionsHolder;
        this.userRegistry = userRegistry;
        this.byteClassLoaderUtil = new ByteClassLoaderUtil();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        Executable executable = byteClassLoaderUtil.defineClass(msg);

        String result = executable.execute(userRegistry, sessionsHolder);

        ctx.writeAndFlush(result);
    }

}
