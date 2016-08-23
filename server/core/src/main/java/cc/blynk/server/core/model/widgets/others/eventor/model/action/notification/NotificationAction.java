package cc.blynk.server.core.model.widgets.others.eventor.model.action.notification;

import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.08.16.
 */
public abstract class NotificationAction extends BaseAction {

    public String message;

    @SuppressWarnings("unchecked")
    public void execute(ChannelHandlerContext ctx, String triggerValue) {
        if (message != null && !message.isEmpty()) {
            //todo refactor, a bit ugly
            BaseSimpleChannelInboundHandler<StringMessage> hardwareHandler = (BaseSimpleChannelInboundHandler) ctx.pipeline().get("HHArdwareHandler");
            hardwareHandler.messageReceived(ctx, makeMessage(triggerValue));
        }
    }

    abstract StringMessage makeMessage(String triggerValue);

}
