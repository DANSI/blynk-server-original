package cc.blynk.server.model.widgets.inputs;

import cc.blynk.server.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class GPS extends Widget {

    public boolean stream;

    public boolean triggerOnEnter;

    public float triggerLat;

    public float triggerLon;

    public int triggerRadius;

    public int accuracy;
}
