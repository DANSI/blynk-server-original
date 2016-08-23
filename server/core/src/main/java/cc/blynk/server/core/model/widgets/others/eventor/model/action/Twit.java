package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.TwitMessage;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Twit extends NotificationAction {

    public String message;

    public Twit() {
    }

    public Twit(String message) {
        this.message = message;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(ChannelHandlerContext ctx, String triggerValue) {
        if (message != null && !message.isEmpty()) {
            //todo refactor, a bit ugly
            BaseSimpleChannelInboundHandler<StringMessage> hardwareHandler = (BaseSimpleChannelInboundHandler) ctx.pipeline().get("HHArdwareHandler");
            hardwareHandler.messageReceived(ctx, new TwitMessage(888, format(message, triggerValue)));
        }
    }
}
