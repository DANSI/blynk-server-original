package cc.blynk.server.application.handlers.main.logic.dashboard;

import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class DeleteDashLogic {

    private static final Logger log = LogManager.getLogger(DeleteDashLogic.class);

    private final UserDao userDao;

    public DeleteDashLogic(UserDao userDao) {
        this.userDao = userDao;
    }

    public void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        int dashId = ParseUtil.parseInt(message.body, message.id);

        int index = user.profile.getDashIndex(dashId, message.id);

        log.info("Deleting dashboard {}.", dashId);

        user.addEnergy(user.profile.dashBoards[index]);
        user.profile.dashBoards = ArrayUtil.remove(user.profile.dashBoards, index);
        userDao.deleteProject(user, dashId);

        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(new ResponseMessage(message.id, OK), ctx.voidPromise());
    }

}
