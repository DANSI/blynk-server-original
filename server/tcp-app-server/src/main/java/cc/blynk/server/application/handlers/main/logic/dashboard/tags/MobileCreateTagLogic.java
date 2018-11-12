package cc.blynk.server.application.handlers.main.logic.dashboard.tags;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
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
public final class MobileCreateTagLogic {

    private static final Logger log = LogManager.getLogger(MobileCreateTagLogic.class);

    private MobileCreateTagLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        String deviceString = split[1];

        if (deviceString == null || deviceString.isEmpty()) {
            throw new IllegalCommandException("Income tag message is empty.");
        }

        Profile profile = user.profile;
        DashBoard dash = profile.getDashByIdOrThrow(dashId);

        Tag newTag = JsonParser.parseTag(deviceString, message.id);

        log.debug("Creating new tag {}.", newTag);

        if (newTag.isNotValid()) {
            throw new IllegalCommandException("Income tag name is not valid.");
        }

        for (Tag tag : dash.tags) {
            if (tag.id == newTag.id || tag.name.equals(newTag.name)) {
                throw new IllegalCommandException("Tag with same id/name already exists.");
            }
        }

        profile.addTag(dash, newTag);
        user.lastModifiedTs = System.currentTimeMillis();

        if (ctx.channel().isWritable()) {
            ctx.writeAndFlush(makeUTF8StringMessage(CREATE_TAG, message.id, newTag.toString()), ctx.voidPromise());
        }
    }

}
