package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareAppLogic {

    private static final Logger log = LogManager.getLogger(HardwareAppLogic.class);

    private final SessionsHolder sessionsHolder;

    public HardwareAppLogic(SessionsHolder sessionsHolder) {
        this.sessionsHolder = sessionsHolder;
    }

    private static boolean pinModeMessage(String body) {
        return body.length() > 0 && body.charAt(0) == 'p';
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, Message message) {
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
