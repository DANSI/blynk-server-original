package cc.blynk.server.handlers.app.main.auth;

import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.protocol.appllication.RegisterMessage;
import cc.blynk.server.dao.UserDao;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static cc.blynk.common.enums.Response.*;
import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * Process register message.
 * Divides input string by spaces on 2 parts:
 * "username" "password".
 * Checks if user not registered yet. If not - registering.
 *
 * For instance, incoming register message may be : "user@mail.ua my_password"
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
@ChannelHandler.Sharable
public class RegisterHandler extends SimpleChannelInboundHandler<RegisterMessage> implements DefaultExceptionHandler {

    private final UserDao userDao;
    private final Set<String> allowedUsers;

    public RegisterHandler(UserDao userDao, String[] allowedUsersArray) {
        this.userDao = userDao;
        if (allowedUsersArray != null && allowedUsersArray.length > 0 &&
                allowedUsersArray[0] != null && !"".equals(allowedUsersArray[0])) {
            allowedUsers = new HashSet<>(Arrays.asList(allowedUsersArray));
            log.debug("Created allowed user list : {}", allowedUsersArray);
        } else {
            allowedUsers = null;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RegisterMessage message) throws Exception {
        //warn: split may be optimized
        String[] messageParts = message.body.split(" ", 2);

        //expecting message with 2 parts, described above in comment.
        if (messageParts.length != 2) {
            log.error("Register Handler. Wrong income message format. {}", message);
            ctx.writeAndFlush(produce(message.id, ILLEGAL_COMMAND));
            return;
        }

        String userName = messageParts[0].toLowerCase();
        String pass = messageParts[1];
        log.info("Trying register user : {}", userName);

        if (!EmailValidator.getInstance().isValid(userName)) {
            log.error("Register Handler. Wrong email: {}", userName);
            ctx.writeAndFlush(produce(message.id, ILLEGAL_COMMAND));
            return;
        }

        if (userDao.isUserExists(userName)) {
            log.warn("User with name {} already exists.", userName);
            ctx.writeAndFlush(produce(message.id, USER_ALREADY_REGISTERED));
            return;
        }

        if (allowedUsers != null && !allowedUsers.contains(userName)) {
            log.warn("User with name {} not allowed to register.", userName);
            ctx.writeAndFlush(produce(message.id, NOT_ALLOWED));
            return;
        }

        userDao.add(userName, pass);

        log.info("Registered {}.", userName);

        ctx.writeAndFlush(produce(message.id, OK));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }

}
