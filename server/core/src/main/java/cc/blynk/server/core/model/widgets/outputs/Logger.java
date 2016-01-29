package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.enums.GraphPeriod;
import cc.blynk.server.core.model.enums.PinType;
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
    public boolean isSame(byte pinIn, PinType pinType) {
        return false;
    }

    @Override
    public String getModeType() {
        return null;
    }

    @Override
    public String makeHardwareBody() {
        return null;
    }
}
