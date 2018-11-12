package cc.blynk.server.application.handlers.main;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.protocol.enums.Command;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.MobileStateHolderUtil.getAppState;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 *
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class MobileChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(MobileChannelStateHandler.class);

    private final SessionDao sessionDao;

    public MobileChannelStateHandler(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        var state = getAppState(ctx.channel());
        if (state != null) {
            var session = sessionDao.get(state.userKey);
            if (session != null) {
                log.trace("Application channel disconnect. {}", ctx.channel());

                for (var dashBoard : state.user.profile.dashBoards) {
                    if (dashBoard.isAppConnectedOn && dashBoard.isActive) {
                        log.trace("{}-{}. Sending App Disconnected event to hardware.",
                                state.user.email, state.user.appName);
                        session.sendMessageToHardware(dashBoard.id, Command.BLYNK_INTERNAL, 7777, "adis");
                    }
                }
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.trace("State handler. App timeout disconnect. Event : {}. Closing.", ((IdleStateEvent) evt).state());
            ctx.close();
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

}
