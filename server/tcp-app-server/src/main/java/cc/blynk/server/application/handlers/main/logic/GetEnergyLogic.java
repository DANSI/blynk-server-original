package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.utils.ByteBufUtil.*;



/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.03.16.
 */
public class GetEnergyLogic {

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        ctx.writeAndFlush(makeStringMessage(GET_ENERGY, message.id, String.valueOf(user.getEnergy())), ctx.voidPromise());
    }

}
