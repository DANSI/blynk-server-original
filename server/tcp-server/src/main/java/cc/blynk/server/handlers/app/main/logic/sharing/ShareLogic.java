package cc.blynk.server.handlers.app.main.logic.sharing;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.exceptions.IllegalCommandBodyException;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;
import static cc.blynk.common.utils.ParseUtil.*;
import static cc.blynk.server.utils.StateHolderUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class ShareLogic {

    private final SessionDao sessionDao;

    public ShareLogic(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] splitted = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        int dashId = parseInt(splitted[0], message.id);
        DashBoard dash = user.profile.getDashById(dashId, message.id);

        switch (splitted[1]) {
            case "on" :
                dash.isShared = true;
                break;
            case "off" :
                dash.isShared = false;
                break;
            default:
                throw new IllegalCommandBodyException("Wrong share command body", message.id);
        }

        Session session = sessionDao.userSession.get(user);
        for (Channel appChannel : session.appChannels) {
            if (appChannel != ctx.channel() && getAppState(appChannel) != null) {
                appChannel.writeAndFlush(message);
            }
        }

        ctx.writeAndFlush(produce(message.id, OK));
    }

}