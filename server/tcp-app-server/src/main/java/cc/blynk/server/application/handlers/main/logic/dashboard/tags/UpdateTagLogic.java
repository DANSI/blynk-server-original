package cc.blynk.server.application.handlers.main.logic.dashboard.tags;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class UpdateTagLogic {

    private static final Logger log = LogManager.getLogger(UpdateTagLogic.class);

    private UpdateTagLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        var split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashId = Integer.parseInt(split[0]);
        var tagString = split[1];

        if (tagString == null || tagString.isEmpty()) {
            throw new IllegalCommandException("Income tag message is empty.");
        }

        var dash = user.profile.getDashByIdOrThrow(dashId);

        var newTag = JsonParser.parseTag(tagString, message.id);

        log.debug("Updating new tag {}.", tagString);

        if (newTag.isNotValid()) {
            throw new IllegalCommandException("Income tag name is not valid.");
        }

        var existingTag = dash.getTagById(newTag.id);

        if (existingTag == null) {
            throw new IllegalCommandException("Attempt to update tag with non existing id.");
        }

        existingTag.update(newTag);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
