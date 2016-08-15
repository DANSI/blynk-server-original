package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.MailMessage;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Mail extends BaseAction {

    public String message;

    public Mail() {
    }

    public Mail(String message) {
        this.message = message;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(ChannelHandlerContext ctx, String triggerValue) {
        if (message != null && !message.isEmpty()) {
            //todo refactor, a bit ugly
            BaseSimpleChannelInboundHandler<StringMessage> hardwareHandler = (BaseSimpleChannelInboundHandler) ctx.pipeline().get("HHArdwareHandler");
            //todo finish, tests
            hardwareHandler.messageReceived(ctx, new MailMessage(888, format(message, triggerValue)));
        }
    }
}
