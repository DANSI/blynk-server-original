package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class TextInput extends OnePinWidget {

    public String defaultValue;

    public String hint;

    public int limit;

    public SendValueTrigger ValueTrigger;

    public FontSize fontSize;

    public int buttonColor;

    public boolean clearFieldOn;

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 400;
    }
}
