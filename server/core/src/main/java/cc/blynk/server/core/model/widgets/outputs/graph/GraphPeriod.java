package cc.blynk.server.core.model.widgets.outputs.graph;

import static cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType.HOURLY;
import static cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType.MINUTE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public enum GraphPeriod {

    LIVE(60, MINUTE),
    ONE_HOUR(60, MINUTE),
    SIX_HOURS(6 * 60, MINUTE),
    DAY(24 * 60, MINUTE),
    WEEK(24 * 7, HOURLY),
    MONTH(30 * 24, HOURLY),
    THREE_MONTHS(3 * 30 * 24, HOURLY),
    ALL(12 * 30 * 24, HOURLY);

    public final int numberOfPoints;
    public final GraphGranularityType granularityType;

    GraphPeriod(int numberOfPoints, GraphGranularityType granularityType) {
        this.numberOfPoints = numberOfPoints;
        this.granularityType = granularityType;
    }
}
