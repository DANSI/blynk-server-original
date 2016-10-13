package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Map extends OnePinWidget {

    public boolean isPinToLatestPoint;

    public String labelFormat;

    public int radius; //zoom level / radius which user selected.

    public float lat; // last user position on map

    public float lon; // last user position on map

    public Point[] points;

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public int getPrice() {
        return 600;
    }

}
