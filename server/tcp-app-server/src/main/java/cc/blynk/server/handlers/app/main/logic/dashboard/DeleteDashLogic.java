package cc.blynk.server.handlers.app.main.logic.dashboard;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.model.auth.User;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class DeleteDashLogic {

    private static final Logger log = LogManager.getLogger(DeleteDashLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        int dashId = ParseUtil.parseInt(message.body, message.id);

        int index = user.profile.getDashIndex(dashId, message.id);

        log.info("Deleting dashboard {}.", dashId);

        user.profile.dashBoards = ArrayUtil.remove(user.profile.dashBoards, index);
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(produce(message.id, OK));
    }

}
