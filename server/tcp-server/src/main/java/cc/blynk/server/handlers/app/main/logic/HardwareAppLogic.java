package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.model.messages.protocol.appllication.sharing.SyncMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.handlers.app.main.auth.AppStateHolder;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.utils.PinUtil.*;
import static cc.blynk.server.utils.StateHolderUtil.*;

/**
 * Responsible for handling incoming hardware commands from applications and forwarding it to
 * appropriate hardware.
 *
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

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.user);

        //if no active dashboards at all - do nothing. this could happen only in case of app. bug
        if (!state.user.hasActive()) {
            //throw new NoActiveDashboardException(message.id);
            return;
        }

        //todo remove on next deployment.
        if (state.isOldAPI() || ("Android".equals(state.osType) && "21".equals(state.version))) {
            if (pinModeMessage(message.body)) {
                log.trace("Pin Mode message catch. Remembering.");
                //check PM command not empty
                if (message.body.length() > 3) {
                    DashBoard dash = state.user.profile.getDashById(state.user.profile.activeDashId, message.id);
                    dash.pinModeMessage = message;
                }
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

            if (isWriteOperation(split[1])) {
                state.user.profile.getDashById(dashId, message.id)
                        .update(split[1], message.id);

                //if dash was shared. check for shared channels
                String sharedToken = state.user.dashShareTokens.get(dashId);
                if (sharedToken != null) {
                    for (Channel appChannel : session.appChannels) {
                        if (appChannel != ctx.channel() && needSync(appChannel, sharedToken)) {
                            appChannel.writeAndFlush(new SyncMessage(message.id, message.body));
                        }
                    }
                }
            }

            session.sendMessageToHardware(dashId, new HardwareMessage(message.id, split[1]));
        }
    }

}
