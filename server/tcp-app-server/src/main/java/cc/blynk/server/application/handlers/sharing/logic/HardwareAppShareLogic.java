package cc.blynk.server.application.handlers.sharing.logic;

import cc.blynk.server.application.handlers.sharing.auth.AppShareStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.FrequencyWidget;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.SyncMessage;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareAppShareLogic {

    private static final Logger log = LogManager.getLogger(HardwareAppShareLogic.class);

    private final SessionDao sessionDao;

    public HardwareAppShareLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppShareStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.user);

        String[] split = message.body.split(StringUtils.BODY_SEPARATOR_STRING, 2);
        int dashId = ParseUtil.parseInt(split[0], message.id);

        DashBoard dashBoard = state.user.profile.getDashById(dashId, message.id);

        if (!dashBoard.isActive) {
            log.debug("No active dashboard.");
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.NO_ACTIVE_DASHBOARD), ctx.voidPromise());
            return;
        }

        if (!dashBoard.isShared) {
            log.debug("Dashboard is not shared. User : {}, {}", state.user.name, ctx.channel().remoteAddress());
            ctx.writeAndFlush(new ResponseMessage(message.id, Response.NOT_ALLOWED), ctx.voidPromise());
            return;
        }

        char operation = split[1].charAt(1);
        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        switch (operation) {
            case 'w':
                dash.update(split[1], message.id);

                String sharedToken = state.user.dashShareTokens.get(dashId);
                if (sharedToken != null) {
                    for (Channel appChannel : session.getAppChannels()) {
                        if (appChannel != ctx.channel() && Session.needSync(appChannel, sharedToken)) {
                            appChannel.writeAndFlush(new SyncMessage(message.id, message.body), appChannel.voidPromise());
                        }
                    }
                }
                session.sendMessageToHardware(ctx, dashId, message.id, HARDWARE, split[1]);
                break;
            case 'r':
                Widget widget = dash.findWidgetByPin(split[1].split(StringUtils.BODY_SEPARATOR_STRING), message.id);
                if (widget == null) {
                    throw new IllegalCommandBodyException("No frequency widget for read command.", message.id);
                }

                if (widget instanceof FrequencyWidget) {
                    if (((FrequencyWidget) widget).isTicked(split[1])) {
                        session.sendMessageToHardware(ctx, dashId, message.id, HARDWARE, split[1]);
                    }
                } else {
                    //corner case for 3-d parties. sometimes users need to read pin state even from non-frequency widgets
                    session.sendMessageToHardware(ctx, dashId, message.id, HARDWARE,  split[1]);
                }
                break;
        }


    }

}
