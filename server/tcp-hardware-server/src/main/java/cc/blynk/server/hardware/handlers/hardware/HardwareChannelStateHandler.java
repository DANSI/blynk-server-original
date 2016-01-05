package cc.blynk.server.hardware.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.ResponseWithBodyMessage;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.utils.StateHolderUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 *
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class HardwareChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(HardwareChannelStateHandler.class);

    private final SessionDao sessionDao;
    private final BlockingIOProcessor blockingIOProcessor;

    public HardwareChannelStateHandler(SessionDao sessionDao, BlockingIOProcessor blockingIOProcessor) {
        this.sessionDao = sessionDao;
        this.blockingIOProcessor = blockingIOProcessor;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        HardwareStateHolder state = getHardState(ctx.channel());
        if (state != null) {
            Session session = sessionDao.userSession.get(state.user);
            if (session != null) {
                session.hardwareChannels.remove(ctx.channel());
                log.trace("Hardware channel disconnect.");
                sentOfflineMessage(state);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            log.trace("Hardware timeout disconnect.");
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    private void sentOfflineMessage(HardwareStateHolder state) {
        DashBoard dashBoard = state.user.profile.getDashById(state.dashId, 0);
        if (dashBoard.isActive) {
            Notification notification = dashBoard.getWidgetByType(Notification.class);
            if (notification == null || !notification.notifyWhenOffline) {
                Session session = sessionDao.userSession.get(state.user);
                if (session.appChannels.size() > 0) {
                    for (Channel appChannel : session.appChannels) {
                        appChannel.writeAndFlush(
                                new ResponseWithBodyMessage(
                                        0, Command.RESPONSE, DEVICE_WENT_OFFLINE_2, state.dashId
                                )
                        );
                    }
                }
            } else {
                String boardType = dashBoard.boardType;
                String dashName = dashBoard.name;
                dashName = dashName == null ? "" : dashName;
                blockingIOProcessor.push(state.user,
                        notification,
                        String.format("Your %s went offline. \"%s\" project is disconnected.", boardType, dashName),
                        state.dashId);
            }
        }
    }


}
