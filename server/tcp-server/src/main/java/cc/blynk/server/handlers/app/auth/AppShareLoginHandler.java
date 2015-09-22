package cc.blynk.server.handlers.app.auth;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;
import cc.blynk.common.model.messages.ResponseMessage;
import cc.blynk.common.model.messages.protocol.appllication.ShareLoginMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.handlers.app.AppShareHandler;
import cc.blynk.server.handlers.common.UserNotLoggerHandler;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * Handler responsible for managing apps sharing login messages.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class AppShareLoginHandler extends SimpleChannelInboundHandler<ShareLoginMessage> {

    private static final Logger log = LogManager.getLogger(AppShareLoginHandler.class);

    private final ServerProperties props;
    private final UserRegistry userRegistry;
    private final SessionsHolder sessionsHolder;
    private final StorageDao storageDao;
    private final HandlerState handlerState;

    public AppShareLoginHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder, StorageDao storageDao, HandlerState handlerState) {
        this.props = props;
        this.userRegistry = userRegistry;
        this.sessionsHolder = sessionsHolder;
        this.storageDao = storageDao;
        this.handlerState = handlerState;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ShareLoginMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ", 2);

        if (messageParts.length == 2) {
            appLogin(ctx, message.id, messageParts[0], messageParts[1]);
        } else {
           throw new IllegalCommandException("Wrong income message format.", message.id);
        }

        ctx.writeAndFlush(produce(message.id, OK));
    }

    private void appLogin(ChannelHandlerContext ctx, int messageId, String username, String token) {
        String userName = username.toLowerCase();

        User user = userRegistry.getUserByToken(token);

        if (user == null || !user.getName().equals(userName)) {
            log.debug("Share token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            ctx.writeAndFlush(new ResponseMessage(messageId, Command.RESPONSE, Response.INVALID_TOKEN));
            return;
        }

        Integer dashId = UserRegistry.getDashIdByToken(user.getDashShareTokens(), token, messageId);

        cleanPipeline(ctx.pipeline());
        ctx.pipeline().addLast(new AppShareHandler(props, userRegistry, sessionsHolder, storageDao, new HandlerState(dashId, user, token)));

        sessionsHolder.addAppChannel(user, ctx.channel());

        log.info("Shared {} app joined.", user.getName());
    }

    private void cleanPipeline(ChannelPipeline pipeline) {
        pipeline.remove(this);
        pipeline.remove(UserNotLoggerHandler.class);
        pipeline.remove(RegisterHandler.class);
        pipeline.remove(AppLoginHandler.class);
    }

}
