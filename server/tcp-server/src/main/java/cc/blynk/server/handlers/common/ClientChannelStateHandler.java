package cc.blynk.server.handlers.common;

import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Notification;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.DEVICE_WENT_OFFLINE;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 *
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class ClientChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(ClientChannelStateHandler.class);

    private final SessionsHolder sessionsHolder;
    private final NotificationsProcessor notificationsProcessor;

    public ClientChannelStateHandler(SessionsHolder sessionsHolder) {
        this.sessionsHolder = sessionsHolder;
        this.notificationsProcessor = null;
    }

    public ClientChannelStateHandler(SessionsHolder sessionsHolder, NotificationsProcessor notificationsProcessor) {
        this.sessionsHolder = sessionsHolder;
        this.notificationsProcessor = notificationsProcessor;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionsHolder.removeFromSession(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            log.trace("Channel was inactive for a long period. Closing...");
            if (ctx.channel().attr(ChannelState.IS_HARD_CHANNEL).get()) {
                User user = ctx.channel().attr(ChannelState.USER).get();
                if (user != null) {
                    Notification notification = user.getProfile().getActiveDashboardWidgetByType(Notification.class);
                    if (notification == null || !notification.notifyWhenOffline) {
                        Session session = sessionsHolder.userSession.get(user);
                        if (session.appChannels.size() > 0) {
                            session.sendMessageToApp(produce(0, DEVICE_WENT_OFFLINE));
                        }
                    } else {
                        String name = user.getProfile().getActiveDashBoard().getName();
                        notificationsProcessor.push(user, notification.token, "Your device '{}' went offline.".replace("{}", name));
                    }
                }

            }
            //channel is already closed here by ReadTimeoutHandler
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }


}
