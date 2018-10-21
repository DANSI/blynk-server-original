package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.utils.StringUtils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.03.16.
 */
public class SegmentedControl extends OnePinWidget {

    public volatile String[] labels;

    public FontSize fontSize;

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 400;
    }

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case LABELS :
                this.labels = propertyValue.split(StringUtils.BODY_SEPARATOR_STRING);
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }
}
