package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Button extends OnePinWidget {

    public boolean pushMode;

    public volatile String onLabel;

    public volatile String offLabel;

    @Override
    public String makeHardwareBody() {
        if (isNotValid() || value == null) {
            return null;
        }
        return makeHardwareBody(pinType, pin, value);
    }

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 200;
    }

    @Override
    public void setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case ON_LABEL :
                this.onLabel = propertyValue;
                break;
            case OFF_LABEL :
                this.offLabel = propertyValue;
                break;
            default:
                super.setProperty(property, propertyValue);
                break;
        }
    }
}
