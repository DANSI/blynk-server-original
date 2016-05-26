package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.utils.LimitedQueue;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Terminal extends OnePinWidget {

    public boolean autoScrollOn;

    public boolean terminalInputOn;

    public boolean textLightOn;

    //todo move 25 to properties
    public transient final List<String> lastCommands = new LimitedQueue<>(25);

    @Override
    public void updateIfSame(byte pin, PinType type, String[] values) {
        if (isSame(pin, type)) {
            this.value = values[0];
            this.lastCommands.add(values[0]);
        }
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
