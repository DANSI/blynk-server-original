package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import io.netty.channel.ChannelHandlerContext;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.08.16.
 */
public abstract class NotificationAction extends BaseAction {

    public abstract void execute(ChannelHandlerContext ctx, String triggerValue);

}
