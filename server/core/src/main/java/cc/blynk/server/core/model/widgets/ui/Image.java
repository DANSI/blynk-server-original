package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.16.
 */
public class Image extends OnePinWidget {

    public ImageSource source;

    public ImageScaling scaling;

    public String[] urls;

    @Override
    public PinMode getModeType() {
        return PinMode.in;
    }

    @Override
    public int getPrice() {
        return 600;
    }
}
