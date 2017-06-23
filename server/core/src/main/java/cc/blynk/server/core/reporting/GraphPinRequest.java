package cc.blynk.server.core.reporting;

import cc.blynk.server.core.model.enums.GraphGranularityType;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.10.15.
 */
public class GraphPinRequest {

    public final int dashId;

    public final int deviceId;

    public final PinType pinType;

    public final byte pin;

    public final int count;

    public final GraphGranularityType type;

    public GraphPinRequest(int dashId, int deviceId, String[] messageParts, int pinIndex, int valuesPerPin) {
        try {
            this.dashId = dashId;
            this.deviceId = deviceId;
            this.pinType = PinType.getPinType(messageParts[pinIndex * valuesPerPin].charAt(0));
            this.pin = Byte.parseByte(messageParts[pinIndex * valuesPerPin + 1]);
            this.count = Integer.parseInt(messageParts[pinIndex * valuesPerPin + 2]);
            this.type = GraphGranularityType.getPeriodByType(messageParts[pinIndex * valuesPerPin + 3].charAt(0));
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("Graph request command body incorrect.");
        }
    }

}
