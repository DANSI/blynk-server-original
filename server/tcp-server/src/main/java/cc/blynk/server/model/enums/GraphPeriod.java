package cc.blynk.server.model.enums;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public enum GraphPeriod {

    DAY(24),
    WEEK(24 * 7),
    MONTH(30 * 24),
    THREE_MONTHS(3 * 30 * 24),
    ALL(100 * 12 * 30 * 24);

    public int periodInHours;

    GraphPeriod(int hours) {
        this.periodInHours = hours;
    }

}
