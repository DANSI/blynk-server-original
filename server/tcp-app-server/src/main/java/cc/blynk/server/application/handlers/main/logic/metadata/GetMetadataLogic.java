package cc.blynk.server.application.handlers.main.logic.metadata;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.BlynkByteBufUtil.makeUTF8StringMessage;

;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class GetMetadataLogic {

    private static final Logger log = LogManager.getLogger(GetMetadataLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String response = JsonParser.toJson(user.profile.metadata);
        ctx.writeAndFlush(makeUTF8StringMessage(Command.GET_METADATA, message.id, response), ctx.voidPromise());
    }

}
