package cc.blynk.server.handlers.hardware;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.ResponseWithBodyMessage;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.widgets.others.Notification;
import cc.blynk.server.workers.notifications.BlockingIOProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.DEVICE_WENT_OFFLINE;
import static cc.blynk.common.enums.Response.DEVICE_WENT_OFFLINE_2;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static cc.blynk.server.utils.HandlerUtil.getState;

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
        sessionDao.removeHardFromSession(ctx.channel());
        log.trace("Hardware channel disconnect.");
        sentOfflineMessage(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            log.trace("Hardware timeout disconnect.");
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    private void sentOfflineMessage(Channel channel) {
        HardwareStateHolder handlerState = getState(channel);
        if (handlerState.user != null) {
            DashBoard dashBoard = handlerState.user.profile.getDashboardById(handlerState.dashId, 0);
            if (dashBoard.isActive) {
                Notification notification = dashBoard.getWidgetByType(Notification.class);
                if (notification == null || !notification.notifyWhenOffline) {
                    Session session = sessionDao.userSession.get(handlerState.user);
                    if (session.appChannels.size() > 0) {
                        for (Channel appChannel : session.appChannels) {
                            HardwareStateHolder appState = getState(appChannel);
                            if (appState.isOldAPI() || ("Android".equals(appState.osType) && "21".equals(appState.version))) {
                                appChannel.writeAndFlush(produce(0, DEVICE_WENT_OFFLINE));
                            } else {
                                appChannel.writeAndFlush(
                                        new ResponseWithBodyMessage(
                                                0, Command.RESPONSE, DEVICE_WENT_OFFLINE_2, handlerState.dashId
                                        )
                                );
                            }
                        }
                    }
                } else {
                    String boardType = dashBoard.boardType;
                    String dashName = dashBoard.name;
                    dashName = dashName == null ? "" : dashName;
                    blockingIOProcessor.push(handlerState.user,
                            notification,
                            String.format("Your %s went offline. \"%s\" project is disconnected.", boardType, dashName),
                            handlerState.dashId);
                }
            }
        }
    }


}
