package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinReadingWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LabeledValueDisplay extends OnePinReadingWidget {

    private TextAlignment textAlignment;

    private String valueFormatting;

    private FontSize fontSize;

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case VALUE_FORMATTING :
                this.valueFormatting = propertyValue;
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }

    @Override
    public PinMode getModeType() {
        return PinMode.in;
    }

    @Override
    public int getPrice() {
        return 400;
    }

}
