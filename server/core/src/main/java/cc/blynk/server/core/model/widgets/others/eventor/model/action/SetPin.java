package cc.blynk.server.core.model.widgets.others.eventor.model.action;

import cc.blynk.server.core.model.Pin;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class SetPin extends BaseAction {

    public Pin pin;

    public String value;

    public SetPin() {
    }

    public SetPin(Pin pin, String value) {
        this.pin = pin;
        this.value = value;
    }

    @Override
    public void execute(ChannelHandlerContext ctx, String triggerValue) {
        if (pin != null && pin.pinType != null && pin.pin > -1 && value != null) {
            String body = Pin.makeHardwareBody(pin.pwmMode, pin.pinType, pin.pin, value);
            ctx.writeAndFlush(makeStringMessage(HARDWARE, 888, body), ctx.voidPromise());
        }
    }

}
