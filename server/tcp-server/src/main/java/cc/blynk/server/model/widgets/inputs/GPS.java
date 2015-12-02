package cc.blynk.server.model.widgets.inputs;

import cc.blynk.server.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class GPS extends OnePinWidget {

    public boolean stream;

    public boolean triggerOnEnter;

    public float triggerLat;

    public float triggerLon;

    public int triggerRadius;

    public int accuracy;
}
