package cc.blynk.server.handlers.app.main.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.appllication.LoadProfileGzippedBinaryMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.model.auth.User;
import cc.blynk.utils.ByteUtils;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class LoadProfileGzippedLogic {

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

        byte[] compressed = ByteUtils.compress(body, message.id);

        ctx.writeAndFlush(new LoadProfileGzippedBinaryMessage(message.id, compressed));
    }

}
