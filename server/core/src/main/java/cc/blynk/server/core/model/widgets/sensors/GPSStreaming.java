package cc.blynk.server.core.model.widgets.sensors;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class GPSStreaming extends OnePinWidget {

     public int accuracy;

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 500;
    }
}
