package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.enums.PinMode;

/**
 * All widgets that doesn't have pins on UI should extend this class.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.04.16.
 */
public abstract class NoPinWidget extends Widget {

    @Override
    public PinMode getModeType() {
        return null;
    }

    @Override
    public void erase() {
    }

    @Override
    public void updateValue(Widget oldWidget) {
    }

    @Override
    public boolean isAssignedToDevice(int deviceId) {
        return false;
    }
}
