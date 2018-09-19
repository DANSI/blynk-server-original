package cc.blynk.server.application.handlers.main.logic.dashboard.tags;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.CREATE_TAG;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class CreateTagLogic {

    private static final Logger log = LogManager.getLogger(CreateTagLogic.class);

    private CreateTagLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        var split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashId = Integer.parseInt(split[0]);
        var deviceString = split[1];

        if (deviceString == null || deviceString.isEmpty()) {
            throw new IllegalCommandException("Income tag message is empty.");
        }

        var dash = user.profile.getDashByIdOrThrow(dashId);

        var newTag = JsonParser.parseTag(deviceString, message.id);

        log.debug("Creating new tag {}.", newTag);

        if (newTag.isNotValid()) {
            throw new IllegalCommandException("Income tag name is not valid.");
        }

        for (Tag tag : dash.tags) {
            if (tag.id == newTag.id || tag.name.equals(newTag.name)) {
                throw new IllegalCommandException("Tag with same id/name already exists.");
            }
        }

        dash.tags = ArrayUtil.add(dash.tags, newTag, Tag.class);
        dash.updatedAt = System.currentTimeMillis();
        user.lastModifiedTs = dash.updatedAt;

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(makeUTF8StringMessage(CREATE_TAG, message.id, newTag.toString()), ctx.voidPromise());
        }
    }

}
