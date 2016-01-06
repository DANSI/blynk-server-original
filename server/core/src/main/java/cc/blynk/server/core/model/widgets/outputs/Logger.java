package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.HardwareBody;
import cc.blynk.server.core.model.enums.GraphPeriod;
import cc.blynk.server.core.model.widgets.MultiPinWidget;

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

    @Override
    public String getModeType() {
        return null;
    }
}
