package cc.blynk.server.core.model.widgets.ui.image;

import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.StringUtils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.16.
 */
public class Image extends OnePinWidget {

    public ImageSource source;

    public ImageScaling scaling;

    public volatile String[] urls;

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
            case URLS :
                this.urls = propertyValue.split(StringUtils.BODY_SEPARATOR_STRING);
                return true;
            case URL :
                String[] split = StringUtils.split2(propertyValue);
                if (split.length == 2) {
                    int index = Integer.parseInt(split[0]) - 1;
                    if (index >= 0 && index < urls.length) {
                        this.urls = ArrayUtil.copyAndReplace(this.urls, split[1], index);
                        return true;
                    }
                }
                return false;
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
