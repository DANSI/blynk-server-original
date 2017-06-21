package cc.blynk.server.core.model.enums;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public enum GraphGranularityType {

    MINUTE('m', 60 * 1000),
    HOURLY('h', 60 * 60 * 1000),
    DAILY('d', 24 * 60 * 60 * 1000);

    public final char type;
    public final long period;

    GraphGranularityType(char type, long period) {
        this.type = type;
        this.period = period;
    }

    public static GraphGranularityType getPeriodByType(char type) {
        for (GraphGranularityType graphGranularityType : values()) {
            if (type == graphGranularityType.type) {
                return graphGranularityType;
            }
        }
        return null;
    }

}
