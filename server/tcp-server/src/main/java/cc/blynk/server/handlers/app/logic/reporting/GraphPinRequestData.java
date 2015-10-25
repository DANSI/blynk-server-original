package cc.blynk.server.handlers.app.logic.reporting;

import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.10.15.
 */
public class GraphPinRequestData extends GraphPinRequest {

    public GraphPinRequestData(String[] messageParts, final int pinIndex, int msgId, int valuesPerPin) {
        try {
            dashId = Integer.parseInt(messageParts[pinIndex * valuesPerPin]);
            pinType = PinType.getPingType(messageParts[pinIndex * valuesPerPin + 1].charAt(0));
            pin = Byte.parseByte(messageParts[pinIndex * valuesPerPin + 2]);
            count = Integer.parseInt(messageParts[pinIndex * valuesPerPin + 3]);
            type = GraphType.getPeriodByType(messageParts[pinIndex * valuesPerPin + 4].charAt(0));
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("HardwareLogic command body incorrect.", msgId);
        }
    }
}
