package cc.blynk.server.core.model.widgets.ui.reporting.type;

public enum DayOfMonth {

    FIRST("at the first day of every month"),
    LAST("at the last day of every month");

    public final String label;

    DayOfMonth(String label) {
        this.label = label;
    }

}
