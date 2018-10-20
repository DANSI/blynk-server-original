package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class NumberInput extends OnePinWidget {

    public String suffix;

    public float step;

    public boolean isLoopOn;

    public FontSize fontSize;

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case SUFFIX :
                this.suffix = propertyValue;
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 400;
    }
}
