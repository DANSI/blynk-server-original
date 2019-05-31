package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31.05.19.
 */
public class Switch extends OnePinWidget {

    public volatile String onLabel;

    public volatile String offLabel;

    public int offBackgroundColor;

    public int onBackgroundColor;

    public int handleColor;

    public FontSize fontSize;

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
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case ON_LABEL :
                this.onLabel = propertyValue;
                return true;
            case OFF_LABEL :
                this.offLabel = propertyValue;
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }
}
