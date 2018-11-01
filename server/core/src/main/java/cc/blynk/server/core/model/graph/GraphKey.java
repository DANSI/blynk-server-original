package cc.blynk.server.core.model.graph;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.utils.NumberUtil;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
public class GraphKey {

    public final int dashId;

    public final short pin;

    public final PinType pinType;

    public final String value;

    public final long ts;

    public GraphKey(int dashId, String[] bodyParts, long ts) {
        this.dashId = dashId;
        this.pinType = PinType.getPinType(bodyParts[0].charAt(0));
        this.pin = NumberUtil.parsePin(bodyParts[1]);
        this.value = bodyParts[2];
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GraphKey)) {
            return false;
        }

        GraphKey graphKey = (GraphKey) o;

        if (dashId != graphKey.dashId) {
            return false;
        }
        if (pin != graphKey.pin) {
            return false;
        }
        return pinType == graphKey.pinType;
    }

    @Override
    public int hashCode() {
        int result = dashId;
        result = 31 * result + (int) pin;
        result = 31 * result + (pinType != null ? pinType.hashCode() : 0);
        return result;
    }
}
