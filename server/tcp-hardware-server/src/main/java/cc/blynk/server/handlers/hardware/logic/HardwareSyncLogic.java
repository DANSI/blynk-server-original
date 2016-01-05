package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.common.model.messages.StringMessage;
import cc.blynk.common.model.messages.protocol.HardwareMessage;
import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.SyncWidget;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.utils.PinUtil;
import io.netty.channel.ChannelHandlerContext;

import java.time.Instant;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareSyncLogic {

    public HardwareSyncLogic() {

    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        final int dashId = state.dashId;
        DashBoard dash = state.user.profile.getDashById(dashId, message.id);

        if (message.length == 0) {
            //return all widgets state
            for (Widget widget : dash.widgets) {
                if (widget instanceof SyncWidget) {
                    ((SyncWidget) widget).send(ctx, message.id);
                }
            }

            ctx.flush();
        } else {
            //return specific widget state
            String[] bodyParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);
            PinType pinType = PinType.getPingType(bodyParts[0].charAt(0));
            byte pin = Byte.parseByte(bodyParts[1]);

            if (PinUtil.isReadOperation(bodyParts[0])) {
                long now = Instant.now().getEpochSecond();
                ctx.writeAndFlush(new HardwareMessage(message.id, Pin.makeHardwareBody(pinType, pin, String.valueOf(now))));
                //todo finish this when we have RTC widget.
                //Widget widget = dash.findWidgetByPin(pin, pinType);
                //if (widget instanceof RTC)  {
                //    long now = System.currentTimeMillis();
                //    ctx.writeAndFlush(new HardwareMessage(message.id, OnePinWidget.makeHardwareBody(pinType, pin, String.valueOf(now))));
                //}
            }
        }
    }

}
