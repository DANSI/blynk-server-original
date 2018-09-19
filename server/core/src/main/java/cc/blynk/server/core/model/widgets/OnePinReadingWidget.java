package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.Channel;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.02.17.
 */
public abstract class OnePinReadingWidget extends OnePinWidget implements FrequencyWidget {

    public int frequency;

    private transient long lastRequestTS;

    @Override
    public boolean isTicked(long now) {
        if (hasReadingInterval() && now >= lastRequestTS + frequency) {
            this.lastRequestTS = now;
            return true;
        }
        return false;
    }

    @Override
    public boolean hasReadingInterval() {
        return frequency > 0;
    }

    @Override
    public int getDeviceId() {
        return deviceId;
    }

    @Override
    public void writeReadingCommand(Channel channel) {
        if (isNotValid()) {
            return;
        }
        StringMessage msg = makeUTF8StringMessage(HARDWARE, READING_MSG_ID,
                DataStream.makeReadingHardwareBody(pinType.pintTypeChar, pin));
        channel.write(msg, channel.voidPromise());
    }

}
