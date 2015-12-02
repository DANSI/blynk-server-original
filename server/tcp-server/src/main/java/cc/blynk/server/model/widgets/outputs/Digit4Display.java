package cc.blynk.server.model.widgets.outputs;

import cc.blynk.server.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Digit4Display extends OnePinWidget implements FrequencyWidget {

    private int frequency;

    private transient long lastRequestTS;

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
}
