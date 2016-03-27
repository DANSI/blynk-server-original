package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.FrequencyWidget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.*;

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

        String[] split = message.body.split(StringUtils.BODY_SEPARATOR_STRING, 2);
        int dashId = ParseUtil.parseInt(split[0], message.id);

        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        //if no active dashboard - do nothing. this could happen only in case of app. bug
        if (!dash.isActive) {
            return;
        }

        final char operation = split[1].charAt(1);
        switch (operation) {
            case 'm' :
                log.trace("Pin Mode message catch. Remembering.");
                //check PM command not empty
                if (split[1].length() > 3) {
                    dash.pinModeMessage = new HardwareMessage(message.id, split[1]);
                }
                session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1]);
                break;
            case 'w' :
                dash.update(split[1], message.id);

                //if dash was shared. check for shared channels
                if (state.user.dashShareTokens != null) {
                    String sharedToken = state.user.dashShareTokens.get(dashId);
                    session.sendToSharedApps(ctx.channel(), sharedToken, SYNC, message.id, message.body);
                }
                session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1]);
                break;
            case 'r' :
                Widget widget = dash.findWidgetByPin(split[1].split(StringUtils.BODY_SEPARATOR_STRING), message.id);
                if (widget == null) {
                    throw new IllegalCommandBodyException("No frequency widget for read command.", message.id);
                }

                if (widget instanceof FrequencyWidget) {
                    if (((FrequencyWidget) widget).isTicked(split[1])) {
                        session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1]);
                    }
                } else {
                    //corner case for 3-d parties. sometimes users need to read pin state even from non-frequency widgets
                    session.sendMessageToHardware(ctx, dashId, HARDWARE, message.id, split[1]);
                }
                break;
        }
    }

}
