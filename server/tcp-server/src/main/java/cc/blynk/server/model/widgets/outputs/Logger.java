package cc.blynk.server.model.widgets.outputs;

import cc.blynk.server.model.HardwareBody;
import cc.blynk.server.model.enums.GraphPeriod;
import cc.blynk.server.model.widgets.MultiPinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public class Logger extends MultiPinWidget {

    public GraphPeriod period;

    public boolean showLegends;

    @Override
    public void updateIfSame(HardwareBody body) {
    }
}
