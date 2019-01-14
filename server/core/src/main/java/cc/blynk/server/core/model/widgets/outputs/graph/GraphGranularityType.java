package cc.blynk.server.core.model.widgets.outputs.graph;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.08.15.
 */
public enum GraphGranularityType {

    MINUTE("minute", 'm', 60 * 1000),
    HOURLY("hourly", 'h', 60 * 60 * 1000),
    DAILY("daily", 'd', 24 * 60 * 60 * 1000);

    public final String label;
    public final char type;
    public final long period;

    private static final GraphGranularityType[] values = values();

    GraphGranularityType(String label, char type, long period) {
        this.label = label;
        this.type = type;
        this.period = period;
    }

    public static GraphGranularityType[] getValues() {
        return values;
    }
}
