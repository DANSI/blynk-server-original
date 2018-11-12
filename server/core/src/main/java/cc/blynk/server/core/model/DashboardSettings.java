package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.Theme;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.04.17.
 */
public final class DashboardSettings {

    public final String name;

    public final boolean isShared;

    public final Theme theme;

    public final boolean keepScreenOn;

    public final boolean isAppConnectedOn;

    public final boolean isNotificationsOff;

    public final boolean widgetBackgroundOn;

    public final int color;

    public final boolean isDefaultColor;

    @JsonCreator
    public DashboardSettings(@JsonProperty("name") String name,
                             @JsonProperty("isShared") boolean isShared,
                             @JsonProperty("theme") Theme theme,
                             @JsonProperty("keepScreenOn") boolean keepScreenOn,
                             @JsonProperty("isAppConnectedOn") boolean isAppConnectedOn,
                             @JsonProperty("isNotificationsOff") boolean isNotificationsOff,
                             @JsonProperty("widgetBackgroundOn") boolean widgetBackgroundOn,
                             @JsonProperty("color") int color,
                             @JsonProperty("isDefaultColor") boolean isDefaultColor) {
        this.name = name;
        this.isShared = isShared;
        this.theme = theme == null ? Theme.Blynk : theme;
        this.keepScreenOn = keepScreenOn;
        this.isAppConnectedOn = isAppConnectedOn;
        this.isNotificationsOff = isNotificationsOff;
        this.widgetBackgroundOn = widgetBackgroundOn;
        this.color = color;
        this.isDefaultColor = isDefaultColor;
    }
}
