package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.04.15.
 */
public class Step extends OnePinWidget {

    public volatile float step;

    public boolean isArrowsOn;

    public boolean isLoopOn;

    public boolean isSendStep;

    public int frequency;

    public boolean showValueOn = true;

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 500;
    }

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case STEP :
                this.step = Float.parseFloat(propertyValue);
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }
}
