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

    public String hint;

    public int limit;

    public FontSize fontSize;

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 400;
    }
}
