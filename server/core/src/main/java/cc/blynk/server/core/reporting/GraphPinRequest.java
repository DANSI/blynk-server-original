package cc.blynk.server.core.reporting;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.outputs.graph.AggregationFunctionType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod;

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

    public final short pin;

    private final GraphPeriod graphPeriod;

    public final AggregationFunctionType functionType;

    public final int count;

    public final GraphGranularityType type;

    public final int skipCount;

    public GraphPinRequest(int dashId, int[] deviceIds, DataStream dataStream,
                           GraphPeriod graphPeriod, int skipCount, AggregationFunctionType function) {
        this.dashId = dashId;
        this.deviceId = -1;
        this.deviceIds = deviceIds == null ? EMPTY_INTS : deviceIds;
        this.isTag = true;
        if (dataStream == null) {
            this.pinType = PinType.VIRTUAL;
            this.pin = (short) DataStream.NO_PIN;
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
            this.pin = (short) DataStream.NO_PIN;
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
