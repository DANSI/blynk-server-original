package cc.blynk.server.core.model.widgets.ui.tiles;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.tiles.templates.ButtonTileTemplate;
import cc.blynk.server.core.model.widgets.ui.tiles.templates.PageTileTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_INTS;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_WIDGETS;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.10.17.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "mode",
        defaultImpl = PageTileTemplate.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PageTileTemplate.class, name = "PAGE"),
        @JsonSubTypes.Type(value = ButtonTileTemplate.class, name = "BUTTON")
})
public abstract class TileTemplate {

    public final long id;

    public volatile Widget[] widgets;

    public final int[] deviceIds;

    public final String templateId;

    public final String name;

    public final String iconName;

    public final String boardType;

    @JsonProperty("pin")
    public final DataStream dataStream;

    public final boolean showDeviceName;

    public TileTemplate(long id,
                        Widget[] widgets,
                        int[] deviceIds,
                        String templateId,
                        String name,
                        String iconName,
                        String boardType,
                        DataStream dataStream,
                        boolean showDeviceName) {
        this.id = id;
        this.widgets = widgets == null ? EMPTY_WIDGETS : widgets;
        this.deviceIds = deviceIds == null ? EMPTY_INTS : deviceIds;
        this.templateId = templateId;
        this.name = name;
        this.iconName = iconName;
        this.boardType = boardType;
        this.dataStream = dataStream;
        this.showDeviceName = showDeviceName;
    }

    public int getPrice() {
        int sum = 0;
        for (Widget widget : widgets) {
            sum += widget.getPrice();
        }
        return sum;
    }

    public void erase() {
        if (dataStream != null) {
            dataStream.value = null;
        }
        for (Widget widget : widgets) {
            widget.erase();
        }
    }

    public int getWidgetIndexByIdOrThrow(long widgetId) {
        return DashBoard.getWidgetIndexByIdOrThrow(widgets, widgetId);
    }
}
