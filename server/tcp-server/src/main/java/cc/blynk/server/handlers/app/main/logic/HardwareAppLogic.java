package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.model.messages.protocol.appllication.sharing.SyncMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.handlers.app.main.auth.AppStateHolder;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.widgets.outputs.FrequencyWidget;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        String[] split = message.body.split(StringUtils.BODY_SEPARATOR_STRING, 2);
        int dashId = ParseUtil.parseInt(split[0], message.id);

        char operation = split[1].charAt(1);

        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        switch (operation) {
            case 'm' :
                log.trace("Pin Mode message catch. Remembering.");
                //check PM command not empty
                if (split[1].length() > 3) {
                    dash.pinModeMessage = new HardwareMessage(message.id, split[1]);
                }
                session.sendMessageToHardware(ctx, dashId, new HardwareMessage(message.id, split[1]));
                break;
            case 'w' :
                dash.update(new HardwareBody(split[1], message.id));

                //if dash was shared. check for shared channels
                String sharedToken = state.user.dashShareTokens.get(dashId);
                if (sharedToken != null) {
                    session.sendToSharedApps(ctx, sharedToken, new SyncMessage(message.id, message.body));
                }
                session.sendMessageToHardware(ctx, dashId, new HardwareMessage(message.id, split[1]));
                break;
            case 'r' :
                FrequencyWidget frequencyWidget = dash.findReadingWidget(split[1], message.id);
                if (frequencyWidget.isTicked()) {
                    session.sendMessageToHardware(ctx, dashId, new HardwareMessage(message.id, split[1]));
                }
                break;
        }
    }

}
