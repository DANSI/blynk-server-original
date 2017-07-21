package cc.blynk.server.core.model.widgets.outputs.graph;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public enum GraphPeriod {

    LIVE(60, GraphGranularityType.MINUTE),
    ONE_HOUR(60, GraphGranularityType.MINUTE),
    SIX_HOURS(6 * 60, GraphGranularityType.MINUTE),
    DAY(24 * 60, GraphGranularityType.MINUTE),
    WEEK(24 * 7, GraphGranularityType.HOURLY),
    MONTH(30 * 24, GraphGranularityType.HOURLY),
    THREE_MONTHS(3 * 30 * 24, GraphGranularityType.HOURLY),
    ALL(12 * 30 * 24, GraphGranularityType.HOURLY);

    public final int numberOfPoints;
    public final GraphGranularityType granularityType;

    GraphPeriod(int numberOfPoints, GraphGranularityType granularityType) {
        this.numberOfPoints = numberOfPoints;
        this.granularityType = granularityType;
    }
}
