package cc.blynk.server.handlers.app.logic;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.exceptions.DeviceNotInNetworkException;
import cc.blynk.server.handlers.hardware.auth.HandlerState;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
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

    private final SessionDao sessionDao;

    public HardwareAppLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    private static boolean pinModeMessage(String body) {
        return body.length() > 0 && body.charAt(0) == 'p';
    }

    public void messageReceived(ChannelHandlerContext ctx, HandlerState state, Message message) {
        Session session = sessionDao.userSession.get(state.user);

        if (!state.user.hasActive()) {
            //throw new NoActiveDashboardException(message.id);
            return;
        }

        //todo remove on next deployment.
        if (state.isOldAPI()) {
            if (pinModeMessage(message.body)) {
                log.trace("Pin Mode message catch. Remembering.");
                //check PM command not empty
                if (message.body.length() > 3) {
                    DashBoard dash = state.user.profile.getDashById(state.user.profile.activeDashId, message.id);
                    dash.pinModeMessage = message;
                }
            }

            if (session.hardwareChannels.size() == 0) {
                throw new DeviceNotInNetworkException(message.id);
            }

            session.sendMessageToHardware(state.user.profile.activeDashId, message);
        } else {
            String[] split = message.body.split(StringUtils.BODY_SEPARATOR_STRING, 2);
            int dashId = ParseUtil.parseInt(split[0], message.id);

            if (pinModeMessage(split[1])) {
                log.trace("Pin Mode message catch. Remembering.");
                //check PM command not empty
                if (split[1].length() > 3) {
                    DashBoard dash = state.user.profile.getDashById(dashId, message.id);
                    dash.pinModeMessage = message;
                }
            }

            if (session.hardwareChannels.size() == 0) {
                throw new DeviceNotInNetworkException(message.id);
            }

            session.sendMessageToHardware(dashId, new HardwareMessage(message.id, split[1]));
        }
    }

}
