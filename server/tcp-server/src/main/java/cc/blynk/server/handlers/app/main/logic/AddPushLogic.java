package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.app.main.auth.AppStateHolder;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.widgets.notifications.Notification;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AddPushLogic {

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] data = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        int dashId = ParseUtil.parseInt(data[0], message.id);
        String uid = data[1];
        String token = data[2];

        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        Notification notification = dash.getWidgetByType(Notification.class);

        if (notification == null) {
            throw new NotAllowedException("No notification widget.", message.id);
        }

        if ("Android".equals(state.osType)) {
            notification.androidTokens.put(uid, token);
        } else {
            notification.iOSTokens.put(uid, token);
        }

        ctx.writeAndFlush(produce(message.id, OK));
    }
}
