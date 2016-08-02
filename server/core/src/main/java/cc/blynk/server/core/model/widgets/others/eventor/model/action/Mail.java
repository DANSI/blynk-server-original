package cc.blynk.server.core.model.widgets.others.eventor.model.action;

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
    public void execute(ChannelHandlerContext ctx) {

    }
}
