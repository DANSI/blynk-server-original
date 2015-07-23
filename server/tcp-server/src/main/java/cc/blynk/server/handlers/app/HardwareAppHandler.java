package cc.blynk.server.handlers.app;

import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class HardwareAppHandler extends BaseSimpleChannelInboundHandler<HardwareMessage> {
    public HardwareAppHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        super(props, userRegistry, sessionsHolder);
    }

    private static boolean pinModeMessage(String body) {
        return body.length() > 0 && body.charAt(0) == 'p';
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, HardwareMessage message) {
        Session session = sessionsHolder.userSession.get(user);

        if (user.getProfile().activeDashId == null) {
            //throw new NoActiveDashboardException(message.id);
            return;
        }

        if (pinModeMessage(message.body)) {
            log.trace("Pin Mode message catch. Remembering.");
            //check PM command not empty
            if (message.body.length() > 3) {
                user.getProfile().pinModeMessage = message;
            }
        }

        if (session.hardwareChannels.size() == 0) {
            throw new DeviceNotInNetworkException(message.id);
        }

        session.sendMessageToHardware(user.getProfile().activeDashId, message);
    }

}
