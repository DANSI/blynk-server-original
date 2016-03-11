package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.HardwareSyncWidget;
import cc.blynk.server.core.model.widgets.others.RTC;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.PinUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import java.time.Instant;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareSyncLogic {

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        final int dashId = state.dashId;
        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        if (message.length == 0) {
            //return all widgets state
            for (Widget widget : dash.widgets) {
                if (widget instanceof HardwareSyncWidget) {
                    ((HardwareSyncWidget) widget).send(ctx, message.id);
                }
            }

            ctx.flush();
        } else {
            //return specific widget state
            String[] bodyParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);
            PinType pinType = PinType.getPinType(bodyParts[0].charAt(0));
            byte pin = Byte.parseByte(bodyParts[1]);

            if (PinUtil.isReadOperation(bodyParts[0])) {
                Widget widget = dash.findWidgetByPin(pin, pinType);
                if (widget instanceof RTC)  {
                    long now = Instant.now().getEpochSecond();
                    ctx.writeAndFlush(new HardwareMessage(message.id, Pin.makeHardwareBody(pinType, pin, String.valueOf(now))), ctx.voidPromise());
                } else if (widget instanceof HardwareSyncWidget) {
                    ((HardwareSyncWidget) widget).send(ctx, message.id);
                    ctx.flush();
                }
            }
        }
    }

}
