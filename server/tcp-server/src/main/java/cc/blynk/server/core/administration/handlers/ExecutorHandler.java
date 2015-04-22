package cc.blynk.server.core.administration.handlers;

import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.core.administration.model.AdminMessage;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.utils.ByteClassLoaderUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ExecutorHandler extends SimpleChannelInboundHandler<AdminMessage> {

    private final ByteClassLoaderUtil byteClassLoaderUtil;
    private final SessionsHolder sessionsHolder;
    private final UserRegistry userRegistry;

    public ExecutorHandler(UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        this.sessionsHolder = sessionsHolder;
        this.userRegistry = userRegistry;
        this.byteClassLoaderUtil = new ByteClassLoaderUtil();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AdminMessage msg) throws Exception {
        Executable executable = byteClassLoaderUtil.defineClass(msg.classBytes);

        List<String> result = executable.execute(userRegistry, sessionsHolder, msg.params);

        for (String s : result) {
            ctx.writeAndFlush(s);
        }
    }

}
