package cc.blynk.server.application.handlers.main.logic.dashboard.tags;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
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
public final class MobileDeleteTagLogic {

    private static final Logger log = LogManager.getLogger(MobileDeleteTagLogic.class);

    private MobileDeleteTagLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        int tagId = Integer.parseInt(split[1]);

        Profile profile = user.profile;
        DashBoard dash = profile.getDashByIdOrThrow(dashId);

        log.debug("Deleting tag with id {}.", tagId);

        profile.deleteTag(dash, tagId);
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
