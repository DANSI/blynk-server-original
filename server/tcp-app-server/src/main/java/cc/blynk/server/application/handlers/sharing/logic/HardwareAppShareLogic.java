package cc.blynk.server.application.handlers.sharing.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.model.messages.protocol.appllication.sharing.SyncMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.application.handlers.sharing.auth.AppShareStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.exceptions.NoActiveDashboardException;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.HardwareBody;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.widgets.outputs.FrequencyWidget;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareAppShareLogic {

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
            throw new NoActiveDashboardException(message.id);
        }

        char operation = split[1].charAt(1);
        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        switch (operation) {
            case 'w':
                dash.update(new HardwareBody(split[1], message.id));

                String sharedToken = state.user.dashShareTokens.get(dashId);
                if (sharedToken != null) {
                    for (Channel appChannel : session.appChannels) {
                        if (appChannel != ctx.channel() && Session.needSync(appChannel, sharedToken)) {
                            appChannel.writeAndFlush(new SyncMessage(message.id, message.body));
                        }
                    }
                }
                session.sendMessageToHardware(ctx, dashId, new HardwareMessage(message.id, split[1]));
                break;
            case 'r':
                FrequencyWidget frequencyWidget = dash.findReadingWidget(split[1], message.id);
                if (frequencyWidget.isTicked()) {
                    session.sendMessageToHardware(ctx, dashId, new HardwareMessage(message.id, split[1]));
                }
                break;
        }


    }

}
