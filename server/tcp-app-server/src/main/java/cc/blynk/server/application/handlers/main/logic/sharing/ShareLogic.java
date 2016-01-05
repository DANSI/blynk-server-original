package cc.blynk.server.application.handlers.main.logic.sharing;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.utils.AppStateHolderUtil.*;

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

        int dashId = ParseUtil.parseInt(splitted[0], message.id);
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

        ctx.writeAndFlush(new ResponseMessage(message.id, OK));
    }

}