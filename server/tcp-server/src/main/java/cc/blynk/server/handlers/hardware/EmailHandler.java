package cc.blynk.server.handlers.hardware;

import cc.blynk.common.model.messages.protocol.hardware.EmailMessage;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.exceptions.NotAllowedException;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.others.Email;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

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

    public EmailHandler(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        super(props, userRegistry, sessionsHolder);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, User user, EmailMessage message) {
        Email email = user.getUserProfile().getActiveDashboardEmailWidget();

        if (email == null) {
            throw new NotAllowedException("User has no email widget on active dashboard.", message.id);
        }

        //todo move to separate thread
        sendEmail();

        ctx.channel().writeAndFlush(produce(message.id, OK));
    }

    private void sendEmail() {

    }


}
