package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Slider extends OnePinWidget {

    public boolean sendOnReleaseOn;

    public int frequency;

    public int maximumFractionDigits;

    public boolean showValueOn = true;

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 200;
    }

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case FRACTION :
                this.maximumFractionDigits = Integer.parseInt(propertyValue);
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }
}
