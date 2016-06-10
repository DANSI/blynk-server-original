package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RGB extends MultiPinWidget implements HardwareSyncWidget {

    public boolean splitMode;

    public boolean sendOnReleaseOn;

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        if (pins == null) {
            return;
        }
        if (splitMode) {
            for (Pin pin : pins) {
                if (pin.notEmpty()) {
                    ctx.write(makeStringMessage(HARDWARE, msgId, pin.makeHardwareBody()), ctx.voidPromise());
                }
            }
        } else {
            if (pins[0].notEmpty()) {
                ctx.write(makeStringMessage(HARDWARE, msgId, makeHardwareBody()), ctx.voidPromise());
            }
        }
    }

    @Override
    public void sendSyncOnActivate(Channel appChannel, int dashId) {
        if (pins == null) {
            return;
        }
        if (splitMode) {
            for (Pin pin : pins) {
                if (pin.notEmpty()) {
                    String body = dashId + StringUtils.BODY_SEPARATOR_STRING + pin.makeHardwareBody();
                    appChannel.write(makeStringMessage(SYNC, 1111, body), appChannel.voidPromise());
                }
            }
        } else {
            if (pins[0].notEmpty()) {
                String body = dashId + StringUtils.BODY_SEPARATOR_STRING + makeHardwareBody();
                appChannel.write(makeStringMessage(SYNC, 1111, body), appChannel.voidPromise());
            }
        }
    }


    public boolean isSplitMode() {
        return splitMode;
    }

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 400;
    }

}
