package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.PinStorageKey;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.HardwareSyncWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.PinUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.BlynkByteBufUtil.makeResponse;
import static cc.blynk.utils.BlynkByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class HardwareSyncLogic {

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        final int dashId = state.dashId;
        final int deviceId = state.deviceId;
        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        if (message.length == 0) {
            //return all widgets state
            for (Widget widget : dash.widgets) {
                if (widget instanceof HardwareSyncWidget) {
                    HardwareSyncWidget hardwareSyncWidget = (HardwareSyncWidget) widget;
                    if (hardwareSyncWidget.isRequiredForSyncAll()) {
                        hardwareSyncWidget.sendHardSync(ctx, message.id, deviceId);
                    }
                }
            }
            for (Map.Entry<PinStorageKey, String> entry : dash.pinsStorage.entrySet()) {
                PinStorageKey key = entry.getKey();
                if (deviceId == key.deviceId) {
                    String body = Pin.makeHardwareBody(key.pinType, key.pin, entry.getValue());
                    ctx.write(makeUTF8StringMessage(HARDWARE, message.id, body), ctx.voidPromise());
                }
            }

            ctx.flush();
        } else {
            //return specific widget state
            String[] bodyParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

            if (bodyParts.length < 2) {
                ctx.writeAndFlush(makeResponse(message.id, Response.ILLEGAL_COMMAND), ctx.voidPromise());
                return;
            }

            PinType pinType = PinType.getPinType(bodyParts[0].charAt(0));

            if (PinUtil.isReadOperation(bodyParts[0])) {
                for (int i = 1; i < bodyParts.length; i++) {
                    byte pin = ParseUtil.parseByte(bodyParts[i]);
                    Widget widget = dash.findWidgetByPin(state.deviceId, pin, pinType);
                    if (widget == null) {
                        String value = dash.pinsStorage.get(new PinStorageKey(deviceId, pinType, pin));
                        if (value != null) {
                            String body = Pin.makeHardwareBody(pinType, pin, value);
                            ctx.write(makeUTF8StringMessage(HARDWARE, message.id, body), ctx.voidPromise());
                        }
                    } else if (widget instanceof HardwareSyncWidget) {
                        ((HardwareSyncWidget) widget).sendHardSync(ctx, message.id, deviceId);
                    }
                }
                ctx.flush();
            }
        }
    }

}
