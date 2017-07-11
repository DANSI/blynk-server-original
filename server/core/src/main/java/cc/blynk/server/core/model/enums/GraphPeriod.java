package cc.blynk.server.core.model.enums;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public enum GraphPeriod {

    LIVE(60, GraphGranularityType.MINUTE),
    ONE_HOUR(60, GraphGranularityType.MINUTE),
    SIX_HOURS(360, GraphGranularityType.MINUTE),
    DAY(24, GraphGranularityType.HOURLY),
    WEEK(24 * 7, GraphGranularityType.HOURLY),
    MONTH(30, GraphGranularityType.DAILY),
    THREE_MONTHS(3 * 30, GraphGranularityType.DAILY),
    ALL(12 * 30, GraphGranularityType.DAILY);

    public final int numberOfPoints;
    public final GraphGranularityType granularityType;

    GraphPeriod(int numberOfPoints, GraphGranularityType granularityType) {
        this.numberOfPoints = numberOfPoints;
        this.granularityType = granularityType;
    }
}
