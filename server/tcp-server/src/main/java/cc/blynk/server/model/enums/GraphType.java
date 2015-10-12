package cc.blynk.server.model.enums;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public enum GraphType {

    MINUTE('m', 60 * 1000),
    HOURLY('h', 60 * 60 * 1000),
    DAILY('d', 24 * 60 * 60 * 1000);

    public char type;
    public long period;

    GraphType(char type, long period) {
        this.type = type;
        this.period = period;
    }

    public static GraphType getPeriodByType(char type) {
        for (GraphType graphType : values()) {
            if (type == graphType.type) {
                return graphType;
            }
        }
        return null;
    }

}
