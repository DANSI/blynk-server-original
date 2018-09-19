package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.storage.PinPropertyStorageKey;
import cc.blynk.server.core.model.storage.PinStorageKey;
import cc.blynk.server.core.model.widgets.HardwareSyncWidget;
import cc.blynk.server.core.model.widgets.others.rtc.RTC;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.illegalCommand;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class HardwareSyncLogic {

    private HardwareSyncLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        var deviceId = state.device.id;
        var dash = state.dash;

        if (message.body.length() == 0) {
            syncAll(ctx, message.id, dash, deviceId);
        } else {
            syncSpecificPins(ctx, message.body, message.id, dash, deviceId);
        }
    }

    private static void syncAll(ChannelHandlerContext ctx, int msgId, DashBoard dash, int deviceId) {
        //return all widgets state
        for (var widget : dash.widgets) {
            //one exclusion, no need to sync RTC
            if (widget instanceof HardwareSyncWidget && !(widget instanceof RTC) && ctx.channel().isWritable()) {
                ((HardwareSyncWidget) widget).sendHardSync(ctx, msgId, deviceId);
            }
        }
        //return all static server holders
        for (var entry : dash.pinsStorage.entrySet()) {
            var key = entry.getKey();
            if (deviceId == key.deviceId && !(key instanceof PinPropertyStorageKey) && ctx.channel().isWritable()) {
                for (String value : entry.getValue().values()) {
                    var body = key.makeHardwareBody(value);
                    ctx.write(makeUTF8StringMessage(HARDWARE, msgId, body), ctx.voidPromise());
                }
            }
        }

        ctx.flush();
    }

    //message format is "vr 22 33"
    //return specific widget state
    private static void syncSpecificPins(ChannelHandlerContext ctx, String messageBody,
                                         int msgId, DashBoard dash, int deviceId) {
        var bodyParts = messageBody.split(StringUtils.BODY_SEPARATOR_STRING);

        if (bodyParts.length < 2 || bodyParts[0].isEmpty()) {
            ctx.writeAndFlush(illegalCommand(msgId), ctx.voidPromise());
            return;
        }

        var pinType = PinType.getPinType(bodyParts[0].charAt(0));

        if (StringUtils.isReadOperation(bodyParts[0])) {
            for (int i = 1; i < bodyParts.length; i++) {
                var pin = Byte.parseByte(bodyParts[i]);
                var widget = dash.findWidgetByPin(deviceId, pin, pinType);
                if (ctx.channel().isWritable()) {
                    if (widget == null) {
                        var pinStorageValue = dash.pinsStorage.get(new PinStorageKey(deviceId, pinType, pin));
                        if (pinStorageValue != null) {
                            for (String value : pinStorageValue.values()) {
                                var body = DataStream.makeHardwareBody(pinType, pin, value);
                                ctx.write(makeUTF8StringMessage(HARDWARE, msgId, body), ctx.voidPromise());
                            }
                        }
                    } else if (widget instanceof HardwareSyncWidget) {
                        ((HardwareSyncWidget) widget).sendHardSync(ctx, msgId, deviceId);
                    }
                }
            }
            ctx.flush();
        }
    }

}
