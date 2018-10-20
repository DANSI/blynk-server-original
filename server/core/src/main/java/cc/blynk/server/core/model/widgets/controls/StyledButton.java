package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.utils.ByteUtils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class StyledButton extends OnePinWidget {

    public boolean pushMode;

    public ButtonState onButtonState;

    public ButtonState offButtonState;

    public FontSize fontSize;

    public Edge edge;

    public ButtonStyle buttonStyle;

    public boolean lockSize;

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
        return 300;
    }

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case ON_BACK_COLOR :
                if (this.onButtonState != null) {
                    this.onButtonState.backgroundColor = ByteUtils.parseColor(propertyValue);
                }
                return true;
            case OFF_BACK_COLOR :
                if (this.offButtonState != null) {
                    this.offButtonState.backgroundColor = ByteUtils.parseColor(propertyValue);
                }
                return true;
            case ON_COLOR :
                if (this.onButtonState != null) {
                    this.onButtonState.textColor = ByteUtils.parseColor(propertyValue);
                }
                return true;
            case OFF_COLOR :
                if (this.offButtonState != null) {
                    this.offButtonState.textColor = ByteUtils.parseColor(propertyValue);
                }
                return true;
            case ON_LABEL :
                if (this.onButtonState != null) {
                    this.onButtonState.text = propertyValue;
                }
                return true;
            case OFF_LABEL :
                if (this.offButtonState != null) {
                    this.offButtonState.text = propertyValue;
                }
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }
}
