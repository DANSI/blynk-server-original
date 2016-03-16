package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class AddPushLogic {

    public static void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String[] data = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        int dashId = ParseUtil.parseInt(data[0], message.id);
        String uid = data[1];
        String token = data[2];

        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        Notification notification = dash.getWidgetByType(Notification.class);

        if (notification == null) {
            throw new NotAllowedException("No notification widget.", message.id);
        }

        switch (state.osType) {
            case ANDROID :
                notification.androidTokens.put(uid, token);
                break;
            case IOS :
                notification.iOSTokens.put(uid, token);
                break;
        }

        ctx.writeAndFlush(new ResponseMessage(message.id, OK), ctx.voidPromise());
    }
}
