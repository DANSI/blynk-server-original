package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_DEVICES;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class GetDevicesLogic {

    private static final Logger log = LogManager.getLogger(GetDevicesLogic.class);

    private GetDevicesLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        var dashId = Integer.parseInt(message.body);

        var dash = user.profile.getDashByIdOrThrow(dashId);

        var response = JsonParser.toJson(dash.devices);
        if (response == null) {
            response = "[]";
        }

        log.debug("Returning devices : {}", response);

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(makeUTF8StringMessage(GET_DEVICES, message.id, response), ctx.voidPromise());
        }
    }

}
