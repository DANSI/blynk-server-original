package cc.blynk.server.application.handlers.main.logic.face;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;

import static cc.blynk.server.core.model.serialization.CopyUtil.copyTags;
import static cc.blynk.server.internal.CommonByteBufUtil.notAllowed;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * Update faces of related project.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileUpdateFaceLogic {

    private static final Logger log = LogManager.getLogger(MobileUpdateFaceLogic.class);

    private MobileUpdateFaceLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       User user, StringMessage message) {
        int parentDashId = Integer.parseInt(message.body);

        DashBoard parent = user.profile.getDashByIdOrThrow(parentDashId);

        HashSet<String> appIds = new HashSet<>();
        for (DashBoard dashBoard : user.profile.dashBoards) {
            if (dashBoard.parentId == parentDashId) {
                for (App app : user.profile.apps) {
                    if (ArrayUtil.contains(app.projectIds, dashBoard.id)) {
                        appIds.add(app.id);
                    }
                }
            }
        }

        if (appIds.size() == 0) {
            log.debug("Passed dash has no childs assigned to any app.");
            ctx.writeAndFlush(notAllowed(message.id));
            return;
        }

        boolean hasFaces = false;
        int count = 0;
        log.info("Updating face {} for user {}-{}. App Ids : {}", parentDashId,
                user.email, user.appName, JsonParser.valueToJsonAsString(appIds));
        for (User existingUser : holder.userDao.users.values()) {
            for (DashBoard existingDash : existingUser.profile.dashBoards) {
                if (existingDash.parentId == parentDashId && (existingUser == user
                        || appIds.contains(existingUser.appName))) {
                    hasFaces = true;
                    //we found child project-face
                    log.debug("Found face for {}-{}.", existingUser.email, existingUser.appName);
                    try {
                        existingDash.updateFaceFields(parent);
                        existingDash.tags = copyTags(parent.tags);
                        existingUser.lastModifiedTs = System.currentTimeMillis();
                        //do not close connection for initiator
                        if (existingUser != user) {
                            holder.sessionDao.closeAppChannelsByUser(new UserKey(existingUser));
                        }
                        count++;
                    } catch (Exception e) {
                        log.error("Error updating face for user {}, dashId {}.",
                                existingUser.email, existingDash.id, e);
                        ctx.writeAndFlush(notAllowed(message.id));
                    }
                }
            }
        }

        if (hasFaces) {
            log.debug("{} faces were updated successfully.", count);
            ctx.writeAndFlush(ok(message.id));
        } else {
            log.info("No child faces found for update.");
            ctx.writeAndFlush(notAllowed(message.id));
        }
    }

}
