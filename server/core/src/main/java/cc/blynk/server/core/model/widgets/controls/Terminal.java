package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Terminal extends OnePinWidget {

    public boolean autoScrollOn;

    public boolean terminalInputOn;

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public String makeHardwareBody() {
        return null;
    }
}
