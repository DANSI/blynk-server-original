package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.enums.TextAlignment;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LabeledValueDisplay extends OnePinWidget implements FrequencyWidget {

    private int frequency;

    private TextAlignment textAlignment;

    private String valueFormatting;

    private transient long lastRequestTS;

    @Override
    public final int getFrequency() {
        return frequency;
    }

    @Override
    public final long getLastRequestTS(String body) {
        return lastRequestTS;
    }

    @Override
    public final void setLastRequestTS(String body, long now) {
        this.lastRequestTS = now;
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
