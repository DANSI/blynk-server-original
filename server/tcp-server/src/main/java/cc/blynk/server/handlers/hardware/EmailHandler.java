package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.hardware.EmailMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.exceptions.ServerBusyException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.handlers.hardware.notifications.MailNotification;
import cc.blynk.server.handlers.hardware.notifications.NotificationBase;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Email;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.Queue;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public class EmailHandler extends BaseSimpleChannelInboundHandler<EmailMessage> {

    private final Queue<NotificationBase> notificationsQueue;

    public EmailHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                        Queue<NotificationBase> notificationsQueue) {
        super(props, userRegistry, sessionsHolder);
        this.notificationsQueue = notificationsQueue;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, EmailMessage message) {
        if (message.body == null || message.body.equals("")) {
            throw new IllegalCommandException("Invalid mail notification body.", message.id);
        }

        //todo finish. for now assume all info is coming from hardware
        Email email = user.getUserProfile().getActiveDashboardEmailWidget();

        if (email == null) {
            throw new NotAllowedException("User has no email widget on active dashboard.", message.id);
        }

        String[] bodyParts = message.body.split("\0");

        if (bodyParts.length != 3) {
            throw new IllegalCommandException("Invalid mail notification body.", message.id);
        }

        String to = bodyParts[0];
        String subj = bodyParts[1];
        String body = bodyParts[2];

        try {
            notificationsQueue.add(new MailNotification(to, subj, body, message.id));
        } catch (IllegalStateException e) {
            throw new ServerBusyException(message.id);
        }

        //todo send response immediately?
        ctx.channel().writeAndFlush(produce(message.id, OK));
    }

}
