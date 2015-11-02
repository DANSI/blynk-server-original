package cc.blynk.server.model.widgets.outputs;

import cc.blynk.server.model.widgets.MultiPinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LCD extends MultiPinWidget {

    public int frequency;

    public boolean advancedMode;

    public String textFormatLine1;

    public String textFormatLine2;

    public boolean textLight;
}
