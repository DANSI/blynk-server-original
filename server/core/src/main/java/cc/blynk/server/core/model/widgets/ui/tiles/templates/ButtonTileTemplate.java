package cc.blynk.server.core.model.widgets.ui.tiles.templates;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.03.18.
 */
public class ButtonTileTemplate extends TileTemplate {

    private final boolean pushMode;

    private final boolean labelsVisibility;

    private final State stateOn;

    private final State stateOff;

    @JsonCreator
    public ButtonTileTemplate(@JsonProperty("id") long id,
                              @JsonProperty("widgets") Widget[] widgets,
                              @JsonProperty("deviceIds") int[] deviceIds,
                              @JsonProperty("templateId") String templateId,
                              @JsonProperty("name") String name,
                              @JsonProperty("iconName") String iconName,
                              @JsonProperty("boardType") BoardType boardType,
                              @JsonProperty("dataStream") DataStream dataStream,
                              @JsonProperty("showDeviceName") boolean showDeviceName,
                              @JsonProperty("pushMode")boolean pushMode,
                              @JsonProperty("labelsVisibility") boolean labelsVisibility,
                              @JsonProperty("stateOn") State stateOn,
                              @JsonProperty("stateOff") State stateOff) {
        super(id, widgets, deviceIds, templateId, name, iconName, boardType, dataStream, showDeviceName);
        this.pushMode = pushMode;
        this.labelsVisibility = labelsVisibility;
        this.stateOn = stateOn;
        this.stateOff = stateOff;
    }
}
