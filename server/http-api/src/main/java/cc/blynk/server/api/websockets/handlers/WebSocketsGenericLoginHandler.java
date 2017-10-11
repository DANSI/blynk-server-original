package cc.blynk.server.api.websockets.handlers;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.GetServerHandler;
import cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler;
import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;
import cc.blynk.server.handlers.common.HardwareNotLoggedHandler;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.server.hardware.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.03.17.
 */
@ChannelHandler.Sharable
public class WebSocketsGenericLoginHandler extends SimpleChannelInboundHandler<LoginMessage>
        implements DefaultExceptionHandler {

    private static final Logger log = LogManager.getLogger(WebSocketsGenericLoginHandler.class);

    private final int hardTimeoutSecs;
    private final HardwareLoginHandler hardwareLoginHandler;
    private final HardwareChannelStateHandler hardwareChannelStateHandler;

    private final AppChannelStateHandler appChannelStateHandler;
    private final AppLoginHandler appLoginHandler;
    private final UserNotLoggedHandler userNotLoggedHandler;
    private final GetServerHandler getServerHandler;

    public WebSocketsGenericLoginHandler(Holder holder, int port) {
        this.hardTimeoutSecs = holder.limits.hardwareIdleTimeout;
        this.hardwareLoginHandler = new HardwareLoginHandler(holder, port);
        this.hardwareChannelStateHandler = new HardwareChannelStateHandler(holder.sessionDao, holder.gcmWrapper);

        this.appChannelStateHandler = new AppChannelStateHandler(holder.sessionDao);
        this.appLoginHandler = new AppLoginHandler(holder);
        this.userNotLoggedHandler = new UserNotLoggedHandler();
        this.getServerHandler = new GetServerHandler(holder);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginMessage message) throws Exception {
        if (message.body.contains(StringUtils.BODY_SEPARATOR_STRING)) {
            initAppPipeline(ctx);
        } else {
            initHardwarePipeline(ctx);
        }
        ctx.fireChannelRead(message);
    }

    private void initAppPipeline(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();

        pipeline.addLast("AChannelState", appChannelStateHandler);
        pipeline.addLast("AGetServer", getServerHandler);
        pipeline.addLast("ALogin", appLoginHandler);
        pipeline.addLast("ANotLogged", userNotLoggedHandler);

        pipeline.remove(this);
    }


    private void initHardwarePipeline(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        if (hardTimeoutSecs > 0) {
            pipeline.addFirst("WSReadTimeout", new ReadTimeoutHandler(hardTimeoutSecs));
        }

        //hardware handlers
        pipeline.addLast("WSChannelState", hardwareChannelStateHandler);
        pipeline.addLast("WSLogin", hardwareLoginHandler);
        pipeline.addLast("WSNotLogged", new HardwareNotLoggedHandler());
        pipeline.remove(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }

}
