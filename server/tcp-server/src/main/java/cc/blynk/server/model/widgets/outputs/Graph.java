package cc.blynk.server.model.widgets.outputs;

import cc.blynk.server.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Graph extends OnePinWidget implements FrequencyWidget {

    public boolean isBar;

    public int frequency;

    public transient long lastRequestTS;

    @Override
    public boolean isTicked() {
        final long now = System.currentTimeMillis();
        if (frequency > 0 && now > lastRequestTS + frequency) {
            lastRequestTS = now;
            return true;
        }
        return false;
    }

}
