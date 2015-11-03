package cc.blynk.server.model.widgets.outputs;

import cc.blynk.server.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.11.15.
 */
public abstract class FrequencyWidget extends Widget {

    public int frequency;

    public transient long lastRequestTS;

}
