package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.notifications.mail.MailWrapper;
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

    private final BlockingIOProcessor blockingIOProcessor;
    private final MailWrapper mailWrapper;
    private final DBManager dbManager;
    private final UserDao userDao;

    public UpdateFaceLogic(Holder holder) {
        this.userDao = holder.userDao;
        this.blockingIOProcessor = holder.blockingIOProcessor;
        this.mailWrapper =  holder.mailWrapper;
        this.dbManager = holder.dbManager;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        int dashId = ParseUtil.parseInt(message.body);

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        HashSet<String> appIds = new HashSet<>();
        for (App app : user.profile.apps) {
            if (ArrayUtil.contains(app.projectIds, dashId)) {
                appIds.add(app.id);
            }
        }

        if (appIds.size() == 0) {
            log.debug("Passed dash is not assigned to any app.");
            ctx.writeAndFlush(notAllowed(message.id));
            return;
        }

        boolean hasFaces = false;
        for (User existingUser : userDao.users.values()) {
            if (existingUser == user || appIds.contains(existingUser.appName)) {
                for (DashBoard existingDash : existingUser.profile.dashBoards) {
                    if (existingDash.parentId == dashId) {
                        hasFaces = true;
                        //we found child project-face
                        try {
                            existingDash.updateFaceFields(dash);
                        } catch (Exception e) {
                            log.error("Error updating face for user {}, dashId {}.", existingUser.email, existingDash.id, e);
                            ctx.writeAndFlush(notAllowed(message.id));
                            return;
                        }
                    }
                }
            }
        }

        if (hasFaces) {
            ctx.writeAndFlush(ok(message.id));
        } else {
            log.info("No child faces found for update.");
            ctx.writeAndFlush(notAllowed(message.id));
        }
    }

}
