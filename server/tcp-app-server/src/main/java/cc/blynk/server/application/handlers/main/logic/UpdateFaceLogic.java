package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;

import static cc.blynk.utils.BlynkByteBufUtil.notAllowed;
import static cc.blynk.utils.BlynkByteBufUtil.ok;

/**
 * Update faces of related project.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class UpdateFaceLogic {

    private static final Logger log = LogManager.getLogger(UpdateFaceLogic.class);

    private final UserDao userDao;

    public UpdateFaceLogic(Holder holder) {
        this.userDao = holder.userDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        int parentDashId = ParseUtil.parseInt(message.body);

        DashBoard dash = user.profile.getDashByIdOrThrow(parentDashId);

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
        log.info("Updating face {} for user {}.", parentDashId, user.email);
        for (User existingUser : userDao.users.values()) {
            for (DashBoard existingDash : existingUser.profile.dashBoards) {
                if (existingDash.parentId == parentDashId && (existingUser == user || appIds.contains(existingUser.appName))) {
                    hasFaces = true;
                    //we found child project-face
                    try {
                        existingDash.updateFaceFields(dash);
                        count++;
                    } catch (Exception e) {
                        log.error("Error updating face for user {}, dashId {}.", existingUser.email, existingDash.id, e);
                        ctx.writeAndFlush(notAllowed(message.id));
                        return;
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
