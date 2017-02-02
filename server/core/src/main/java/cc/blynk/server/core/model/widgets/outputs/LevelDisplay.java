package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.widgets.FrequencyWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LevelDisplay extends OnePinWidget implements FrequencyWidget {

    private int frequency;

    private transient long lastRequestTS;

    private boolean isAxisFlipOn;

    @Override
    public final int getFrequency() {
        return frequency;
    }

    @Override
    public final long getLastRequestTS() {
        return lastRequestTS;
    }

    @Override
    public final void setLastRequestTS(long now) {
        this.lastRequestTS = now;
    }

    @Override
    public void sendReadingCommand(Session session, int dashId) {
        session.sendMessageToHardware(dashId, HARDWARE, READING_MSG_ID, Pin.makeReadingHardwareBody(pinType.pintTypeChar, pin), deviceId);
    }

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public int getPrice() {
        return 200;
    }
}
