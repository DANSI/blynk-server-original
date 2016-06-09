package cc.blynk.server.application.handlers.main.logic.metadata;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class SaveMetadataLogic {

    private static final Logger log = LogManager.getLogger(SaveMetadataLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String metadataString = message.body;

        if (metadataString == null || metadataString.equals("")) {
            throw new IllegalCommandException("Metadata message is empty.");
        }

        log.debug("Trying to parse user metadata : {}", metadataString);
        try {
            user.profile.metadata = JsonParser.mapper.readValue(metadataString, Map.class);
            user.lastModifiedTs = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error parsing metadata {}", metadataString);
            ctx.writeAndFlush(makeResponse(message.id, Response.ILLEGAL_COMMAND_BODY), ctx.voidPromise());
            return;
        }
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
