package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.widgets.NoPinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.02.17.
 */
public class DeviceSelector extends NoPinWidget {

    public volatile int value = 0;

    @Override
    public int getPrice() {
        return 1500;
    }

}
