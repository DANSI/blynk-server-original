package cc.blynk.server.core.model;

import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.serialization.View;
import cc.blynk.server.core.model.storage.DashPinStorageKeyDeserializer;
import cc.blynk.server.core.model.storage.PinStorageValueDeserializer;
import cc.blynk.server.core.model.storage.key.DashPinPropertyStorageKey;
import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.widgets.MobileSyncWidget;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.Tile;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.netty.channel.Channel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static cc.blynk.server.core.model.widgets.MobileSyncWidget.ANY_TARGET;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_APPS;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_DASHBOARDS;
import static cc.blynk.utils.StringUtils.truncateFileName;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:04
 */
public class Profile {

    public volatile DashBoard[] dashBoards = EMPTY_DASHBOARDS;

    public volatile App[] apps = EMPTY_APPS;

    @JsonView(View.Private.class)
    @JsonDeserialize(keyUsing = DashPinStorageKeyDeserializer.class,
                     contentUsing = PinStorageValueDeserializer.class)
    public final Map<DashPinStorageKey, PinStorageValue> pinsStorage = new HashMap<>();

    //todo this method is very wrong, need to something with it.
    private static final DashBoard EMPTY_DASH = new DashBoard();
    public DashBoard getFirstDashOrEmpty() {
        if (dashBoards.length == 0) {
            return EMPTY_DASH;
        }
        return dashBoards[0];
    }

    public String getDeviceName(DashBoard dash, int deviceId) {
        Device device = getDeviceById(dash, deviceId);
        if (device != null) {
            return truncateFileName(device.name);
        }
        return "";
    }

    public String getCSVDeviceName(DashBoard dash, int deviceId) {
        Device device = getDeviceById(dash, deviceId);
        if (device == null) {
            return String.valueOf(deviceId);
        }

        String deviceName = device.name;
        if (deviceName == null || deviceName.isEmpty()) {
            return String.valueOf(deviceId);
        }

        return StringUtils.escapeCSV(deviceName);
    }

    public Device getDeviceById(DashBoard dash, int id) {
        for (Device device : dash.devices) {
            if (device.id == id) {
                return device;
            }
        }
        return null;
    }

    public void addDevice(DashBoard dash, Device device) {
        dash.devices = ArrayUtil.add(dash.devices, device, Device.class);
    }

    public Device deleteDevice(DashBoard dash, int deviceId) {
        int existingDeviceIndex = getDeviceIndexByIdOrThrow(dash, deviceId);
        Device deviceToRemove = dash.devices[existingDeviceIndex];
        dash.devices = ArrayUtil.remove(dash.devices, existingDeviceIndex, Device.class);
        dash.eraseWidgetValuesForDevice(deviceId);
        return deviceToRemove;
    }

    private int getDeviceIndexByIdOrThrow(DashBoard dash, int id) {
        Device[] devices = dash.devices;
        for (int i = 0; i < devices.length; i++) {
            if (devices[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Device with passed id not found.");
    }

    public void setOfflineDevice(DashBoard dash) {
        Device[] devices = dash.devices;
        if (devices != null) {
            for (Device device : devices) {
                device.status = Status.OFFLINE;
            }
        }
    }

    public void deleteTag(DashBoard dash, int tagId) {
        int existingTagIndex = getTagIndexByIdOrThrow(dash, tagId);
        dash.tags = ArrayUtil.remove(dash.tags, existingTagIndex, Tag.class);
    }

    public void addTag(DashBoard dash, Tag newTag) {
        dash.tags = ArrayUtil.add(dash.tags, newTag, Tag.class);
    }

    private int getTagIndexByIdOrThrow(DashBoard dash, int id) {
        for (int i = 0; i < dash.tags.length; i++) {
            if (dash.tags[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Tag with passed id not found.");
    }

    public Tag getTagById(DashBoard dash, int id) {
        for (Tag tag : dash.tags) {
            if (tag.id == id) {
                return tag;
            }
        }
        return null;
    }

    public void deleteDeviceFromTags(DashBoard dash, int deviceId) {
        for (Tag tag : dash.tags) {
            tag.deleteDevice(deviceId);
        }
    }

    public void cleanPinStorage(DashBoard dash, Widget widget, boolean removeTemplates) {
        cleanPinStorageInternalWithoutUpdatedAt(dash, widget, true, removeTemplates);
        dash.updatedAt = System.currentTimeMillis();
    }

    public void cleanPinStorageForTileTemplate(DashBoard dash, TileTemplate tileTemplate,
                                               boolean removeProperties) {
        for (int deviceId : tileTemplate.deviceIds) {
            for (Widget widget : tileTemplate.widgets) {
                if (widget instanceof OnePinWidget) {
                    OnePinWidget onePinWidget = (OnePinWidget) widget;
                    cleanPinStorage(dash, onePinWidget, deviceId, removeProperties);
                } else if (widget instanceof MultiPinWidget) {
                    MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
                    cleanPinStorage(dash, multiPinWidget, deviceId, removeProperties);
                }
            }
        }
    }

    private void cleanPinStorage(DashBoard dash,
                                        MultiPinWidget multiPinWidget, int targetId, boolean removeProperties) {
        if (multiPinWidget.dataStreams != null) {
            for (DataStream dataStream : multiPinWidget.dataStreams) {
                if (dataStream != null && dataStream.isValid()) {
                    removePinStorageValue(dash, targetId == -1 ? multiPinWidget.deviceId : targetId,
                            dataStream.pinType, dataStream.pin, removeProperties);
                }
            }
        }
    }

    private void cleanPinStorage(DashBoard dash, OnePinWidget onePinWidget,
                                 int targetId, boolean removeProperties) {
        if (onePinWidget.isValid()) {
            removePinStorageValue(dash, targetId == -1 ? onePinWidget.deviceId : targetId,
                    onePinWidget.pinType, onePinWidget.pin, removeProperties);
        }
    }

    private void removePinStorageValue(DashBoard dash, int targetId,
                                       PinType pinType, short pin, boolean removeProperties) {
        Target target;
        if (targetId < Tag.START_TAG_ID) {
            target = getDeviceById(dash, targetId);
        } else if (targetId < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
            target = getTagById(dash, targetId);
        } else {
            //means widget assigned to device selector widget.
            target = dash.getDeviceSelector(targetId);
        }
        if (target != null) {
            for (int deviceId : target.getAssignedDeviceIds()) {
                pinsStorage.remove(new DashPinStorageKey(dash.id, deviceId, pinType, pin));
                if (removeProperties) {
                    for (WidgetProperty widgetProperty : WidgetProperty.getValues()) {
                        pinsStorage.remove(
                                new DashPinPropertyStorageKey(dash.id, deviceId, pinType, pin, widgetProperty));
                    }
                }
            }
        }
    }

    public void sendAppSyncs(DashBoard dash, Channel appChannel, int targetId) {
        sendPinStorageSyncs(dash, appChannel, targetId);
        for (Widget widget : dash.widgets) {
            if (widget instanceof MobileSyncWidget && appChannel.isWritable()) {
                ((MobileSyncWidget) widget).sendAppSync(appChannel, dash.id, targetId);
            }
        }
    }

    private void sendPinStorageSyncs(DashBoard dash, Channel appChannel, int targetId) {
        for (Map.Entry<DashPinStorageKey, PinStorageValue> entry : pinsStorage.entrySet()) {
            DashPinStorageKey key = entry.getKey();
            if ((targetId == ANY_TARGET || targetId == key.deviceId)
                    && dash.id == key.dashId
                    && appChannel.isWritable()) {
                PinStorageValue pinStorageValue = entry.getValue();
                pinStorageValue.sendAppSync(appChannel, dash.id, key);
            }
        }
    }

    public void cleanPinStorage(DashBoard dash, boolean removeProperties,
                                boolean eraseTemplates) {
        for (Widget widget : dash.widgets) {
            cleanPinStorageInternalWithoutUpdatedAt(dash, widget, removeProperties, eraseTemplates);
        }
        dash.updatedAt = System.currentTimeMillis();
    }

    private void cleanPinStorageInternalWithoutUpdatedAt(DashBoard dash, Widget widget,
                                                         boolean removeProperties, boolean eraseTemplates) {
        if (widget instanceof OnePinWidget) {
            OnePinWidget onePinWidget = (OnePinWidget) widget;
            cleanPinStorage(dash, onePinWidget, -1, removeProperties);
        } else if (widget instanceof MultiPinWidget) {
            MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
            cleanPinStorage(dash, multiPinWidget, -1, removeProperties);
        } else if (widget instanceof DeviceTiles) {
            DeviceTiles deviceTiles = (DeviceTiles) widget;
            cleanPinStorage(dash.id, deviceTiles, removeProperties);
            if (eraseTemplates) {
                cleanPinStorageForTemplate(dash, deviceTiles, removeProperties);
            }
        }
    }

    private void cleanPinStorage(int dashId, DeviceTiles deviceTiles, boolean removeProperties) {
        for (Tile tile : deviceTiles.tiles) {
            if (tile != null && tile.isValidDataStream()) {
                DataStream dataStream = tile.dataStream;
                pinsStorage.remove(new DashPinStorageKey(dashId, tile.deviceId, dataStream.pinType, dataStream.pin));
                if (removeProperties) {
                    for (WidgetProperty widgetProperty : WidgetProperty.getValues()) {
                        pinsStorage.remove(new DashPinPropertyStorageKey(dashId, tile.deviceId,
                                dataStream.pinType, dataStream.pin, widgetProperty));
                    }
                }
            }
        }
    }

    private void cleanPinStorageForTemplate(DashBoard dash,
                                                   DeviceTiles deviceTiles, boolean removeProperties) {
        for (TileTemplate tileTemplate : deviceTiles.templates) {
            cleanPinStorageForTileTemplate(dash, tileTemplate, removeProperties);
        }
    }

    public void cleanPinStorageForDevice(int deviceId) {
        pinsStorage.entrySet().removeIf(entry -> entry.getKey().deviceId == deviceId);
    }

    public void update(DashBoard dash, int deviceId, short pin, PinType pinType, String value, long now) {
        if (!dash.updateWidgets(deviceId, pin, pinType, value)) {
            //special case. #237 if no widget - storing without widget.
            putPinStorageValue(dash, deviceId, pinType, pin, value);
        }

        dash.updatedAt = now;
    }

    public void putPinPropertyStorageValue(DashBoard dash, int deviceId, PinType type, short pin,
                                           WidgetProperty property, String value) {
        putPinStorageValue(dash, new DashPinPropertyStorageKey(dash.id, deviceId, type, pin, property), value);
    }

    private void putPinStorageValue(DashBoard dash, int deviceId, PinType type, short pin, String value) {
        putPinStorageValue(dash, new DashPinStorageKey(dash.id, deviceId, type, pin), value);
    }

    private void putPinStorageValue(DashBoard dash, DashPinStorageKey key, String value) {
        PinStorageValue pinStorageValue = pinsStorage.get(key);
        if (pinStorageValue == null) {
            pinStorageValue = dash.initStorageValueForStorageKey(key);
            pinsStorage.put(key, pinStorageValue);
        }
        pinStorageValue.update(value);
    }

    public Widget getWidgetWithLoggedPin(DashBoard dash, int deviceId, short pin, PinType pinType) {
        for (Widget widget : dash.widgets) {
            if (widget instanceof Superchart) {
                Superchart graph = (Superchart) widget;
                if (isWithinGraph(dash, graph, pin, pinType, deviceId)) {
                    return graph;
                }
            }
            if (widget instanceof DeviceTiles) {
                DeviceTiles deviceTiles = (DeviceTiles) widget;
                for (TileTemplate tileTemplate : deviceTiles.templates) {
                    for (Widget tilesWidget : tileTemplate.widgets) {
                        if (tilesWidget instanceof Superchart) {
                            Superchart graph = (Superchart) tilesWidget;
                            if (isWithinGraph(dash, graph, pin, pinType, deviceId, tileTemplate.deviceIds)) {
                                return graph;
                            }
                        }
                    }
                }
            }
            if (widget instanceof ReportingWidget) {
                ReportingWidget reportingWidget = (ReportingWidget) widget;
                if (reportingWidget.hasPin(pin, pinType)) {
                    return reportingWidget;
                }
            }
        }
        return null;
    }

    private boolean isWithinGraph(DashBoard dash, Superchart graph,
                                         short pin, PinType pinType, int deviceId, int... deviceIds) {
        for (GraphDataStream graphDataStream : graph.dataStreams) {
            if (graphDataStream != null && graphDataStream.dataStream != null
                    && graphDataStream.dataStream.isSame(pin, pinType)) {

                int graphTargetId = graphDataStream.targetId;

                //this is the case when datastream assigned directly to the device
                if (deviceId == graphTargetId) {
                    return true;
                }

                //this is the case when graph is within deviceTiles
                if (deviceIds != null && ArrayUtil.contains(deviceIds, deviceId)) {
                    return true;
                }

                //this is the case when graph is within device selector or tags
                Target target;
                if (graphTargetId < Tag.START_TAG_ID) {
                    target = getDeviceById(dash, graphTargetId);
                } else if (graphTargetId < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                    target = getTagById(dash, graphTargetId);
                } else {
                    //means widget assigned to device selector widget.
                    target = dash.getDeviceSelector(graphTargetId);
                }
                if (target != null && target.contains(deviceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getDashIndexOrThrow(int dashId) {
        for (int i = 0; i < dashBoards.length; i++) {
            if (dashBoards[i].id == dashId) {
                return i;
            }
        }
        throw new IllegalCommandException("Dashboard with passed id not found.");
    }

    public DashBoard getDashByIdOrThrow(int id) {
        for (DashBoard dashBoard : dashBoards) {
            if (dashBoard.id == id) {
                return dashBoard;
            }
        }
        throw new IllegalCommandException("Dashboard with passed id not found.");
    }

    public DashBoard getDashById(int id) {
        for (DashBoard dashBoard : dashBoards) {
            if (dashBoard.id == id) {
                return dashBoard;
            }
        }
        return null;
    }

    public int getAppIndexById(String id) {
        for (int i = 0; i < apps.length; i++) {
            if (apps[i].id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public App getAppById(String id) {
        for (App app : apps) {
            if (app.id.equals(id)) {
                return app;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Profile that = (Profile) o;

        return Arrays.equals(dashBoards, that.dashBoards);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(dashBoards);
    }
}
