package cc.blynk.server.core.model.widgets.ui.image;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
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

    public volatile int opacity;

    public volatile int scale;

    public volatile int rotation;

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case OPACITY :
                this.opacity = Integer.parseInt(propertyValue);
                return true;
            case SCALE :
                this.scale = Integer.parseInt(propertyValue);
                return true;
            case ROTATION :
                this.rotation = Integer.parseInt(propertyValue);
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
        return 600;
    }
}
