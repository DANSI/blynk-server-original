package cc.blynk.server.core.model.widgets.ui.tiles.templates;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.18.
 */
public class PageTileTemplate extends TileTemplate {

    private final String valueName;

    private final String valueFormatting;

    private final String valueSuffix;

    public final int color;

    public final int tileColor;

    private final FontSize fontSize;

    private final boolean showTileLabel;

    private final int maximumFractionDigits;

    @JsonCreator
    public PageTileTemplate(@JsonProperty("id") long id,
                            @JsonProperty("widgets") Widget[] widgets,
                            @JsonProperty("deviceIds") int[] deviceIds,
                            @JsonProperty("templateId") String templateId,
                            @JsonProperty("name") String name,
                            @JsonProperty("iconName") String iconName,
                            @JsonProperty("boardType") BoardType boardType,
                            @JsonProperty("dataStream") DataStream dataStream,
                            @JsonProperty("showDeviceName") boolean showDeviceName,
                            @JsonProperty("valueName") String valueName,
                            @JsonProperty("valueFormatting") String valueFormatting,
                            @JsonProperty("valueSuffix") String valueSuffix,
                            @JsonProperty("color") int color,
                            @JsonProperty("tileColor")int tileColor,
                            @JsonProperty("fontSize") FontSize fontSize,
                            @JsonProperty("showTileLabel")boolean showTileLabel,
                            @JsonProperty("maximumFractionDigits") int maximumFractionDigits) {
        super(id, widgets, deviceIds, templateId, name, iconName, boardType, dataStream, showDeviceName);
        this.valueName = valueName;
        this.valueFormatting = valueFormatting;
        this.valueSuffix = valueSuffix;
        this.color = color;
        this.tileColor = tileColor;
        this.fontSize = fontSize;
        this.showTileLabel = showTileLabel;
        this.maximumFractionDigits = maximumFractionDigits;
    }
}
