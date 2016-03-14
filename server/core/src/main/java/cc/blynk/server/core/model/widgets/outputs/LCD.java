package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.widgets.MultiPinWidget;

import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LCD extends MultiPinWidget implements FrequencyWidget {

    public boolean advancedMode;

    public String textFormatLine1;

    public String textFormatLine2;

    //todo remove after migration.
    public boolean textLight;

    public boolean textLightOn;

    private int frequency;

    private transient Map<String, Long> lastRequestTS = new HashMap<>();

    @Override
    public final int getFrequency() {
        return frequency;
    }

    @Override
    public final long getLastRequestTS(String body) {
        return lastRequestTS.getOrDefault(body, 0L);
    }

    @Override
    public final void setLastRequestTS(String body, long now) {
        this.lastRequestTS.put(body, now);
    }

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public int getPrice() {
        return 400;
    }
}
