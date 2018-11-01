package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.storage.key.DashPinPropertyStorageKey;
import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.widgets.HardwareSyncWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.others.rtc.RTC;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.utils.NumberUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

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
        int deviceId = state.device.id;
        DashBoard dash = state.dash;

        if (message.body.length() == 0) {
            syncAll(ctx, message.id, state.user.profile, dash, deviceId);
        } else {
            syncSpecificPins(ctx, message.body, message.id, state.user.profile, dash, deviceId);
        }
    }

    private static void syncAll(ChannelHandlerContext ctx, int msgId, Profile profile, DashBoard dash, int deviceId) {
        //return all widgets state
        for (Widget widget : dash.widgets) {
            //one exclusion, no need to sync RTC
            if (widget instanceof HardwareSyncWidget && !(widget instanceof RTC) && ctx.channel().isWritable()) {
                ((HardwareSyncWidget) widget).sendHardSync(ctx, msgId, deviceId);
            }
        }

        for (Map.Entry<DashPinStorageKey, PinStorageValue> entry : profile.pinsStorage.entrySet()) {
            DashPinStorageKey key = entry.getKey();
            if (deviceId == key.deviceId
                    && dash.id == key.dashId
                    && !(key instanceof DashPinPropertyStorageKey)
                    && ctx.channel().isWritable()) {
                for (String value : entry.getValue().values()) {
                    String body = key.makeHardwareBody(value);
                    ctx.write(makeUTF8StringMessage(HARDWARE, msgId, body), ctx.voidPromise());
                }
            }
        }

        ctx.flush();
    }

    //message format is "vr 22 33"
    //return specific widget state
    private static void syncSpecificPins(ChannelHandlerContext ctx, String messageBody,
                                         int msgId, Profile profile, DashBoard dash, int deviceId) {
        String[] bodyParts = messageBody.split(StringUtils.BODY_SEPARATOR_STRING);

        if (bodyParts.length < 2 || bodyParts[0].isEmpty()) {
            ctx.writeAndFlush(illegalCommand(msgId), ctx.voidPromise());
            return;
        }

        PinType pinType = PinType.getPinType(bodyParts[0].charAt(0));

        if (StringUtils.isReadOperation(bodyParts[0])) {
            for (int i = 1; i < bodyParts.length; i++) {
                short pin = NumberUtil.parsePin(bodyParts[i]);
                Widget widget = dash.findWidgetByPin(deviceId, pin, pinType);
                if (ctx.channel().isWritable()) {
                    if (widget == null) {
                        PinStorageValue pinStorageValue =
                                profile.pinsStorage.get(new DashPinStorageKey(dash.id, deviceId, pinType, pin));
                        if (pinStorageValue != null) {
                            for (String value : pinStorageValue.values()) {
                                String body = DataStream.makeHardwareBody(pinType, pin, value);
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
