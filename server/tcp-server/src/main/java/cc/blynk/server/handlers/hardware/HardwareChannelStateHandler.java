package cc.blynk.server.handlers.hardware;

import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Notification;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.DEVICE_WENT_OFFLINE;
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

    private final SessionsHolder sessionsHolder;
    private final NotificationsProcessor notificationsProcessor;

    public HardwareChannelStateHandler(SessionsHolder sessionsHolder, NotificationsProcessor notificationsProcessor) {
        this.sessionsHolder = sessionsHolder;
        this.notificationsProcessor = notificationsProcessor;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionsHolder.removeHardFromSession(ctx.channel());
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
        User user = getState(channel).user;
        if (user != null) {
            Notification notification = user.getProfile().getActiveDashboardWidgetByType(Notification.class);
            if (notification == null || !notification.notifyWhenOffline) {
                Session session = sessionsHolder.userSession.get(user);
                if (session.appChannels.size() > 0) {
                    session.sendMessageToApp(produce(0, DEVICE_WENT_OFFLINE));
                }
            } else {
                String boardType = user.getProfile().getActiveDashBoard().getBoardType();
                String dashName = user.getProfile().getActiveDashBoard().getName();
                dashName = dashName == null ? "" : dashName;
                notificationsProcessor.push(user, notification,
                        String.format("Your %s went offline. \"%s\" project is disconnected.", boardType, dashName));
            }
        }
    }


}
