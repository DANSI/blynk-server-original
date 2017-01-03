package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.utils.ParseUtil;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.02.17.
 */
public class DeviceSelector extends NoPinWidget {

    public volatile int[] value = new int[] {0};

    public void updateValue(String currentDeviceIdString) {
        this.value = new int[] {ParseUtil.parseInt(currentDeviceIdString)};
    }

    @Override
    public int getPrice() {
        return 1500;
    }

}
