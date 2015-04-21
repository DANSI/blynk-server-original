package cc.blynk.server.handlers.administration;

import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.utils.ByteClassLoaderUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ExecutorHandler extends SimpleChannelInboundHandler<AdminMessage> {

    private final Logger log = LogManager.getLogger(ExecutorHandler.class);

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

        log.info("Sending back '{}'.", result);

        for (String s : result) {
            ctx.writeAndFlush(s);
        }
    }

}
