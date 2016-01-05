package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.LoadProfileMessage;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@Deprecated
public class LoadProfileLogic {

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String body;
        //load all
        if (message.length == 0) {
            body = user.profile.toString();
        } else {
            //load specific by id
            int dashId = ParseUtil.parseInt(message.body, message.id);
            body = user.profile.getDashById(dashId, message.id).toString();
        }

        ctx.writeAndFlush(new LoadProfileMessage(message.id, body));
    }

}
