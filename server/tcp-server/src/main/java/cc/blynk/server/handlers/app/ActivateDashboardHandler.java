package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.protocol.appllication.ActivateDashboardMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class ActivateDashboardHandler extends BaseSimpleChannelInboundHandler<ActivateDashboardMessage> {

    public ActivateDashboardHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        super(props, userRegistry, sessionsHolder);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, ActivateDashboardMessage message) {
        String dashBoardIdString = message.body;

        int dashBoardId;
        try {
            dashBoardId = Integer.parseInt(dashBoardIdString);
        } catch (NumberFormatException ex) {
            throw new IllegalCommandException(String.format("Dash board id '%s' not valid.", dashBoardIdString), message.id);
        }

        log.debug("Activating dash {} for user {}", dashBoardIdString, user.getName());
        user.getProfile().validateDashId(dashBoardId, message.id);
        user.getProfile().setActiveDashId(dashBoardId);

        ctx.writeAndFlush(produce(message.id, OK));
    }

}
