package cc.blynk.server.core.model.widgets.ui.tiles;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.TextAlignment;
import cc.blynk.server.internal.EmptyArraysUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.10.17.
 */
public class TileTemplate {

    public final long id;

    public final Widget[] widgets;

    public final int[] deviceIds;

    public final String name;

    public final TileMode mode;

    @JsonProperty("pin")
    public final DataStream dataStream;

    public final String valueName;

    public final String valueSuffix;

    public final int color;

    public final TextAlignment alignment;

    public final boolean disableWhenOffline;

    public final boolean showDeviceName;

    @JsonCreator
    public TileTemplate(@JsonProperty("id") long id,
                        @JsonProperty("widgets") Widget[] widgets,
                        @JsonProperty("deviceIds") int[] deviceIds,
                        @JsonProperty("name") String name,
                        @JsonProperty("mode") TileMode mode,
                        @JsonProperty("pin") DataStream dataStream,
                        @JsonProperty("valueName") String valueName,
                        @JsonProperty("valueSuffix") String valueSuffix,
                        @JsonProperty("color") int color,
                        @JsonProperty("alignment") TextAlignment alignment,
                        @JsonProperty("disableWhenOffline") boolean disableWhenOffline,
                        @JsonProperty("showDeviceName") boolean showDeviceName) {
        this.id = id;
        this.widgets = widgets;
        this.deviceIds = deviceIds == null ? EmptyArraysUtil.EMPTY_INTS : deviceIds;
        this.name = name;
        this.mode = mode;
        this.dataStream = dataStream;
        this.valueName = valueName;
        this.valueSuffix = valueSuffix;
        this.color = color;
        this.alignment = alignment;
        this.disableWhenOffline = disableWhenOffline;
        this.showDeviceName = showDeviceName;
    }
}
