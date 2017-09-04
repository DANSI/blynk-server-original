package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.widgets.HardwareSyncWidget;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.StringJoiner;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.BlynkByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RGB extends MultiPinWidget implements HardwareSyncWidget {

    public boolean splitMode;

    public boolean sendOnReleaseOn;

    public int frequency;

    @Override
    public void sendHardSync(ChannelHandlerContext ctx, int msgId, int deviceId) {
        if (dataStreams == null || this.deviceId != deviceId) {
            return;
        }
        if (isSplitMode()) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.notEmpty()) {
                    ctx.write(makeUTF8StringMessage(HARDWARE, msgId,
                            dataStream.makeHardwareBody()), ctx.voidPromise());
                }
            }
        } else {
            if (dataStreams[0].notEmpty()) {
                ctx.write(makeUTF8StringMessage(HARDWARE, msgId,
                        dataStreams[0].makeHardwareBody()), ctx.voidPromise());
            }
        }
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
        if (dataStreams == null) {
            return;
        }
        if (targetId == ANY_TARGET || this.deviceId == targetId) {
            if (isSplitMode()) {
                for (DataStream dataStream : dataStreams) {
                    if (dataStream.notEmpty()) {
                        String body = prependDashIdAndDeviceId(dashId, deviceId, dataStream.makeHardwareBody());
                        appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body),
                                appChannel.voidPromise());
                    }
                }
            } else {
                if (dataStreams[0].notEmpty()) {
                    String body = prependDashIdAndDeviceId(dashId, deviceId, dataStreams[0].makeHardwareBody());
                    appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body),
                            appChannel.voidPromise());
                }
            }
        }
    }

    @Override
    public String getJsonValue() {
        if (dataStreams == null) {
            return "[]";
        }

        if (isSplitMode()) {
            return super.getJsonValue();
        } else {
            StringJoiner sj = new StringJoiner(",", "[", "]");
            if (dataStreams[0].notEmpty()) {
                for (String pinValue : dataStreams[0].value.split(StringUtils.BODY_SEPARATOR_STRING)) {
                    sj.add("\"" + pinValue + "\"");
                }
            }
            return sj.toString();
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
