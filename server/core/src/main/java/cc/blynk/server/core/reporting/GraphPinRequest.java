package cc.blynk.server.core.reporting;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.AggregationFunctionType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;

import java.util.Arrays;

import static cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod.LIVE;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_INTS;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.10.15.
 */
public class GraphPinRequest {

    public final int dashId;

    public final int deviceId;

    public final int[] deviceIds;

    public final boolean isTag;

    public final PinType pinType;

    public final byte pin;

    private final GraphPeriod graphPeriod;

    public final AggregationFunctionType functionType;

    public final int count;

    public final GraphGranularityType type;

    public final int skipCount;

    //todo remove in future versions
    public GraphPinRequest(int dashId, int deviceId, String[] messageParts, int pinIndex, int valuesPerPin) {
        try {
            this.dashId = dashId;
            this.deviceId = deviceId;
            this.pinType = PinType.getPinType(messageParts[pinIndex * valuesPerPin].charAt(0));
            this.pin = Byte.parseByte(messageParts[pinIndex * valuesPerPin + 1]);

            //not used for old graphs, so setting any value.
            this.graphPeriod = GraphPeriod.ALL;
            this.functionType = null;
            this.deviceIds = EMPTY_INTS;
            this.isTag = false;
            /////////////////////////////////////

            this.count = Integer.parseInt(messageParts[pinIndex * valuesPerPin + 2]);
            this.type = GraphGranularityType.getPeriodByType(messageParts[pinIndex * valuesPerPin + 3].charAt(0));
            this.skipCount = 0;
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("Graph request command body incorrect.");
        }
    }

    public GraphPinRequest(int dashId, int[] deviceIds, DataStream dataStream,
                           GraphPeriod graphPeriod, int skipCount, AggregationFunctionType function) {
        this.dashId = dashId;
        this.deviceId = -1;
        this.deviceIds = deviceIds == null ? EMPTY_INTS : deviceIds;
        this.isTag = true;
        if (dataStream == null) {
            this.pinType = PinType.VIRTUAL;
            this.pin = (byte) DataStream.NO_PIN;
        } else {
            this.pinType = (dataStream.pinType == null ? PinType.VIRTUAL : dataStream.pinType);
            this.pin = dataStream.pin;
        }
        this.graphPeriod = graphPeriod;
        this.functionType = function;
        this.count = graphPeriod.numberOfPoints;
        this.type = graphPeriod.granularityType;
        this.skipCount = skipCount;
    }

    public GraphPinRequest(int dashId, int deviceId, DataStream dataStream,
                           GraphPeriod graphPeriod, int skipCount, AggregationFunctionType function) {
        this.dashId = dashId;
        this.deviceId = deviceId;
        this.deviceIds = EMPTY_INTS;
        this.isTag = false;
        if (dataStream == null) {
            this.pinType = PinType.VIRTUAL;
            this.pin = (byte) DataStream.NO_PIN;
        } else {
            this.pinType = (dataStream.pinType == null ? PinType.VIRTUAL : dataStream.pinType);
            this.pin = dataStream.pin;
        }
        this.graphPeriod = graphPeriod;
        this.functionType = (function == null ? AggregationFunctionType.AVG : function);
        this.count = graphPeriod.numberOfPoints;
        this.type = graphPeriod.granularityType;
        this.skipCount = skipCount;
    }

    public boolean isLiveData() {
        return graphPeriod == LIVE;
    }

    public boolean isValid() {
        return deviceId != -1 || deviceIds.length > 0;
    }

    @Override
    public String toString() {
        return "GraphPinRequest{"
                + "dashId=" + dashId
                + ", deviceId=" + deviceId
                + ", deviceIds=" + Arrays.toString(deviceIds)
                + ", isTag=" + isTag
                + ", pinType=" + pinType
                + ", pin=" + pin
                + ", graphPeriod=" + graphPeriod
                + ", functionType=" + functionType
                + ", count=" + count
                + ", type=" + type
                + ", skipCount=" + skipCount
                + '}';
    }
}
