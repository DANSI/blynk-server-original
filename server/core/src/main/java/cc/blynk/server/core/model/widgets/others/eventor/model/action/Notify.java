package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.PushMessage;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Notify extends BaseAction {

    public String message;

    public Notify() {
    }

    public Notify(String message) {
        this.message = message;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(ChannelHandlerContext ctx) {
        if (message != null && !message.isEmpty()) {
            //todo refactor, a bit ugly
            BaseSimpleChannelInboundHandler<StringMessage> hardwareHandler = (BaseSimpleChannelInboundHandler) ctx.pipeline().get("HHArdwareHandler");
            hardwareHandler.messageReceived(ctx, new PushMessage(888, message));
        }
    }
}
