package cc.blynk.server.hardware.handlers.hardware;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.ResponseWithBodyMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.notifications.push.GCMMessage;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.ios.IOSGCMMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Response.*;
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
    private final GCMWrapper gcmWrapper;

    public HardwareChannelStateHandler(SessionDao sessionDao, BlockingIOProcessor blockingIOProcessor, GCMWrapper gcmWrapper) {
        this.sessionDao = sessionDao;
        this.blockingIOProcessor = blockingIOProcessor;
        this.gcmWrapper = gcmWrapper;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        HardwareStateHolder state = getHardState(ctx.channel());
        if (state != null) {
            Session session = sessionDao.userSession.get(state.user);
            if (session != null) {
                session.removeHardChannel(ctx.channel());
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
                if (session.getAppChannels().size() > 0) {
                    for (Channel appChannel : session.getAppChannels()) {
                        appChannel.writeAndFlush(
                                new ResponseWithBodyMessage(
                                        0, Command.RESPONSE, DEVICE_WENT_OFFLINE, state.dashId
                                ),
                                appChannel.voidPromise()
                        );
                    }
                }
            } else {
                String boardType = dashBoard.boardType;
                String dashName = dashBoard.name;
                dashName = dashName == null ? "" : dashName;
                push(state.user, notification,
                        String.format("Your %s went offline. \"%s\" project is disconnected.", boardType, dashName),
                        state.dashId);
            }
        }
    }

    private void push(User user, Notification widget, String body, int dashId) {
        if (widget.androidTokens.size() != 0) {
            for (Map.Entry<String, String> entry : widget.androidTokens.entrySet()) {
                push(user, new AndroidGCMMessage(entry.getValue(), widget.priority, body, dashId), widget.androidTokens, entry.getKey());
            }
        }

        if (widget.iOSTokens.size() != 0) {
            for (Map.Entry<String, String> entry : widget.iOSTokens.entrySet()) {
                push(user, new IOSGCMMessage(entry.getValue(), widget.priority, body, dashId), widget.iOSTokens, entry.getKey());
            }
        }
    }

    private void push(User user, GCMMessage message, Map<String, String> tokens, String uid) {
        blockingIOProcessor.execute(() -> {
            try {
                gcmWrapper.send(message);
            } catch (Exception e) {
                log.error("Error sending push notification on offline hardware. For user {}. {}", user.name, e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("NotRegistered")) {
                    log.error("Removing invalid token. UID {}", uid);
                    tokens.remove(uid);
                }
            }
        });
    }

}
