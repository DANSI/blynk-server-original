package cc.blynk.server.core.model;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.storage.PinPropertyStorageKey;
import cc.blynk.server.core.model.storage.PinStorageKey;
import cc.blynk.server.core.model.storage.PinStorageKeyDeserializer;
import cc.blynk.server.core.model.storage.PinStorageValue;
import cc.blynk.server.core.model.storage.PinStorageValueDeserializer;
import cc.blynk.server.core.model.storage.SinglePinStorageValue;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.Tile;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cc.blynk.server.core.model.widgets.AppSyncWidget.ANY_TARGET;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_DEVICES;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_TAGS;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_WIDGETS;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:04
 */
public class DashBoard {

    //-1 means this is not child project
    private static final int IS_PARENT_DASH = -1;

    public int id;

    public int parentId = IS_PARENT_DASH;

    public boolean isPreview;

    public volatile String name;

    public long createdAt;

    public volatile long updatedAt;

    public volatile Widget[] widgets = EMPTY_WIDGETS;

    public volatile Device[] devices = EMPTY_DEVICES;

    public volatile Tag[] tags = EMPTY_TAGS;

    public volatile Theme theme = Theme.Blynk;

    public volatile boolean keepScreenOn;

    public volatile boolean isAppConnectedOn;

    public volatile boolean isNotificationsOff;

    public volatile boolean isShared;

    public volatile boolean isActive;

    public volatile String sharedToken;

    @JsonDeserialize(keyUsing = PinStorageKeyDeserializer.class,
                     contentUsing = PinStorageValueDeserializer.class)
    public Map<PinStorageKey, PinStorageValue> pinsStorage = Collections.emptyMap();

    public void update(int deviceId, byte pin, PinType pinType, String value, long now) {
        if (!updateWidgets(deviceId, pin, pinType, value)) {
            //special case. #237 if no widget - storing without widget.
            putPinStorageValue(deviceId, pinType, pin, value);
        }

        this.updatedAt = now;
    }

    private boolean updateWidgets(int deviceId, byte pin, PinType type, String value) {
        boolean hasWidget = false;
        for (Widget widget : widgets) {
            if (widget.updateIfSame(deviceId, pin, type, value)) {
                hasWidget = true;
            }
        }
        return hasWidget;
    }

    public String getNameOrEmpty() {
        return name == null ? "" : name;
    }

    public void putPinPropertyStorageValue(int deviceId, PinType type, byte pin,
                                           WidgetProperty property, String value) {
        putPinStorageValue(new PinPropertyStorageKey(deviceId, type, pin, property), value);
    }

    private void putPinStorageValue(int deviceId, PinType type, byte pin, String value) {
        putPinStorageValue(new PinStorageKey(deviceId, type, pin), value);
    }

    private void putPinStorageValue(PinStorageKey key, String value) {
        if (pinsStorage == Collections.EMPTY_MAP) {
            pinsStorage = new HashMap<>();
        }

        PinStorageValue pinStorageValue = pinsStorage.get(key);
        if (pinStorageValue == null) {
            pinStorageValue = initStorageValueForStorageKey(key);
            pinsStorage.put(key, pinStorageValue);
        }
        pinStorageValue.update(value);
    }

    private PinStorageValue initStorageValueForStorageKey(PinStorageKey key) {
        var widget = getWidgetForStorageKey(key);
        if (widget == null) {
            return new SinglePinStorageValue();
        }
        return widget.getPinStorageValue();
    }

    private Widget getWidgetForStorageKey(PinStorageKey key) {
        for (var widget : widgets) {
            if (widget instanceof OnePinWidget) {
                var onePinWidget = (OnePinWidget) widget;
                //pim matches and widget assigned to device selector
                if (onePinWidget.isAssignedToDeviceSelector() && key.isSamePin(onePinWidget)) {
                    var deviceSelector = getDeviceSelector(onePinWidget.deviceId);
                    if (deviceSelector != null && ArrayUtil.contains(deviceSelector.deviceIds, key.deviceId)) {
                        return widget;
                    }
                }
            } else if (widget instanceof MultiPinWidget) {
                var multiPinWidget = (MultiPinWidget) widget;
                if (multiPinWidget.isAssignedToDeviceSelector() && key.isSamePin(multiPinWidget)) {
                    var deviceSelector = getDeviceSelector(multiPinWidget.deviceId);
                    if (deviceSelector != null && ArrayUtil.contains(deviceSelector.deviceIds, key.deviceId)) {
                        return widget;
                    }
                }
            } else if (widget instanceof DeviceTiles) {
                var deviceTiles = (DeviceTiles) widget;
                for (var template : deviceTiles.templates) {
                    if (ArrayUtil.contains(template.deviceIds, key.deviceId)) {
                        for (var tileWidget : template.widgets) {
                            if (tileWidget instanceof OnePinWidget) {
                                if (key.isSamePin((OnePinWidget) tileWidget)) {
                                    return tileWidget;
                                }
                            } else if (tileWidget instanceof MultiPinWidget) {
                                if (key.isSamePin((MultiPinWidget) tileWidget)) {
                                    return tileWidget;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void activate() {
        isActive = true;
        updatedAt = System.currentTimeMillis();
    }

    public void deactivate() {
        isActive = false;
        updatedAt = System.currentTimeMillis();
    }

    public Widget findWidgetByPin(int deviceId, String[] splitted) {
        PinType type = PinType.getPinType(splitted[0].charAt(0));
        byte pin = Byte.parseByte(splitted[1]);
        return findWidgetByPin(deviceId, pin, type);
    }

    public Widget findWidgetByPin(int deviceId, byte pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget.isSame(deviceId, pin, pinType)) {
                return widget;
            }
        }
        return null;
    }

    public WebHook findWebhookByPin(int deviceId, byte pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget instanceof WebHook) {
                WebHook webHook = (WebHook) widget;
                if (webHook.isSameWebHook(deviceId, pin, pinType)) {
                    return webHook;
                }
            }
        }
        return null;
    }

    public boolean needRawDataForGraph(int deviceId, byte pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget instanceof EnhancedHistoryGraph) {
                if (((EnhancedHistoryGraph) widget).hasPin(deviceId, pin, pinType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getWidgetIndexByIdOrThrow(Widget[] widgets, long id) {
        for (int i = 0; i < widgets.length; i++) {
            if (widgets[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Widget with passed id not found.");
    }

    public int getWidgetIndexByIdOrThrow(long id) {
        return getWidgetIndexByIdOrThrow(widgets, id);
    }

    public int getTagIndexById(int id) {
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Tag with passed id not found.");
    }

    public Tag getTagById(int id) {
        for (Tag tag : tags) {
            if (tag.id == id) {
                return tag;
            }
        }
        return null;
    }

    public int getDeviceIndexById(int id) {
        for (int i = 0; i < devices.length; i++) {
            if (devices[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Device with passed id not found.");
    }

    private boolean hasWidgetsByDeviceId(int deviceId) {
        for (Widget widget : widgets) {
            if (widget instanceof OnePinWidget) {
                OnePinWidget onePinWidget = (OnePinWidget) widget;
                if (onePinWidget.deviceId == deviceId) {
                    return true;
                }
            }
            if (widget instanceof MultiPinWidget) {
                MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
                if (multiPinWidget.deviceId == deviceId) {
                    return true;
                }
            }
            if (widget instanceof DeviceSelector) {
                DeviceSelector deviceSelector = (DeviceSelector) widget;
                if (ArrayUtil.contains(deviceSelector.deviceIds, deviceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns list of device ids that should receive user command.
     * Widget could be assigned to specific device or to tag that
     * is assigned to few devices or to device selector widget.
     *
     * @param targetId - deviceId or tagId or device selector widget id
     */
    public Target getTarget(int targetId) {
        if (targetId < Tag.START_TAG_ID) {
            return getDeviceById(targetId);
        } else if (targetId < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
            return getTagById(targetId);
        } else {
            //means widget assigned to device selector widget.
            return getDeviceSelector(targetId);
        }
    }

    public Device getDeviceById(int id) {
        for (Device device : devices) {
            if (device.id == id) {
                return device;
            }
        }
        return null;
    }

    private DeviceSelector getDeviceSelector(long targetId) {
        Widget widget = getWidgetById(targetId);
        if (widget instanceof DeviceSelector) {
            return (DeviceSelector) widget;
        }
        return null;
    }

    public Widget getWidgetByIdOrThrow(long id) {
        return widgets[getWidgetIndexByIdOrThrow(id)];
    }

    public Widget getWidgetById(long id) {
        return getWidgetById(widgets, id);
    }

    public Widget getWidgetByIdInDeviceTilesOrThrow(long id) {
        for (Widget widget : widgets) {
            if (widget instanceof DeviceTiles) {
                DeviceTiles deviceTiles = (DeviceTiles) widget;
                Widget widgetInDeviceTiles = deviceTiles.getWidgetById(id);
                if (widgetInDeviceTiles != null) {
                    return widgetInDeviceTiles;
                }
            }
        }
        throw new IllegalCommandException("Widget with passed id not found.");
    }

    private static Widget getWidgetById(Widget[] widgets, long id) {
        for (Widget widget : widgets) {
            if (widget.id == id) {
                return widget;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getWidgetByType(Class<T> clazz) {
        for (Widget widget : widgets) {
            if (clazz.isInstance(widget)) {
                return (T) widget;
            }
        }
        return null;
    }

    public String buildPMMessage(int deviceId) {
        StringBuilder sb = new StringBuilder("pm");
        for (Widget widget : widgets) {
            widget.append(sb, deviceId);
        }
        if (sb.length() == 2) {
            return null;
        }
        return sb.toString();
    }


    public int energySum() {
        //means this is app preview project so do no manipulation with energy
        if (parentId != IS_PARENT_DASH) {
            return 0;
        }
        int sum = 0;
        for (Widget widget : widgets) {
            sum += widget.getPrice();
        }
        return sum;
    }

    public void eraseValues() {
        this.pinsStorage = Collections.emptyMap();
        for (Widget widget : widgets) {
            widget.erase();
        }
    }

    public void addTimers(TimerWorker timerWorker, UserKey userKey) {
        for (Widget widget : widgets) {
            if (widget instanceof DeviceTiles) {
                DeviceTiles deviceTiles = (DeviceTiles) widget;
                deviceTiles.addTimers(timerWorker, userKey, id);
            } else if (widget instanceof Timer) {
                timerWorker.add(userKey, (Timer) widget, id, -1L, -1L);
            } else if (widget instanceof Eventor) {
                timerWorker.add(userKey, (Eventor) widget, id);
            }
        }
    }

    public void cleanPinStorage(Widget widget, boolean removePropertiesToo) {
        cleanPinStorageInternalWihtoutUpdatedAt(widget, removePropertiesToo);
        this.updatedAt = System.currentTimeMillis();
    }

    private void cleanPinStorage(Widget[] widgets) {
        for (Widget widget : widgets) {
            cleanPinStorageInternalWihtoutUpdatedAt(widget, false);
        }
        this.updatedAt = System.currentTimeMillis();
    }

    public void cleanPinStorageInternalWihtoutUpdatedAt(Widget widget, boolean removePropertiesToo) {
        if (widget instanceof OnePinWidget) {
            OnePinWidget onePinWidget = (OnePinWidget) widget;
            if (onePinWidget.pinType != null) {
                pinsStorage.remove(new PinStorageKey(onePinWidget.deviceId, onePinWidget.pinType, onePinWidget.pin));
                if (removePropertiesToo) {
                    cleanPropertyStorageForTarget(onePinWidget.deviceId, onePinWidget.pinType, onePinWidget.pin);
                }
            }
        } else if (widget instanceof MultiPinWidget) {
            MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
            if (multiPinWidget.dataStreams != null) {
                for (DataStream dataStream : multiPinWidget.dataStreams) {
                    if (dataStream != null && dataStream.pinType != null) {
                        pinsStorage.remove(new PinStorageKey(multiPinWidget.deviceId,
                                dataStream.pinType, dataStream.pin));
                        if (removePropertiesToo) {
                            cleanPropertyStorageForTarget(multiPinWidget.deviceId, dataStream.pinType, dataStream.pin);
                        }
                    }
                }
            }
        } else if (widget instanceof DeviceTiles) {
            DeviceTiles deviceTiles = (DeviceTiles) widget;
            for (Tile tile : deviceTiles.tiles) {
                if (tile.isValidDataStream()) {
                    DataStream dataStream = tile.dataStream;
                    pinsStorage.remove(new PinStorageKey(tile.deviceId, dataStream.pinType, dataStream.pin));
                    if (removePropertiesToo) {
                        cleanPropertyStorageForTarget(tile.deviceId, dataStream.pinType, dataStream.pin);
                    }
                }
            }
        }
    }

    private void cleanPropertyStorageForTarget(int widgetDeviceId, PinType pinType, byte pin) {
        Target target = getTarget(widgetDeviceId);
        if (target != null) {
            for (int deviceId : target.getDeviceIds()) {
                for (WidgetProperty widgetProperty : WidgetProperty.values()) {
                    pinsStorage.remove(new PinPropertyStorageKey(deviceId, pinType, pin, widgetProperty));
                }
            }
        }
    }

    public void sendSyncs(Channel appChannel, int targetId) {
        sendSyncs(appChannel, id, targetId, widgets, pinsStorage);
    }

    private static void sendSyncs(Channel appChannel, int dashId, int targetId,
                                 Widget[] widgets, Map<PinStorageKey, PinStorageValue> pinsStorage) {
        for (Widget widget : widgets) {
            if (widget instanceof AppSyncWidget && appChannel.isWritable()) {
                ((AppSyncWidget) widget).sendAppSync(appChannel, dashId, targetId);
            }
        }

        for (Map.Entry<PinStorageKey, PinStorageValue> entry : pinsStorage.entrySet()) {
            PinStorageKey key = entry.getKey();
            if ((targetId == ANY_TARGET || targetId == key.deviceId) && appChannel.isWritable()) {
                for (String value : entry.getValue().values()) {
                    StringMessage byteBuf = key.toStringMessage(dashId, value);
                    appChannel.write(byteBuf, appChannel.voidPromise());
                }
            }
        }
    }

    //todo add DashboardSettings as Dashboard field
    public void updateSettings(DashboardSettings settings) {
        this.name = settings.name;
        this.isShared = settings.isShared;
        this.theme = settings.theme;
        this.keepScreenOn = settings.keepScreenOn;
        this.isAppConnectedOn = settings.isAppConnectedOn;
        this.isNotificationsOff = settings.isNotificationsOff;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateFields(DashBoard updatedDashboard) {
        this.name = updatedDashboard.name;
        this.isShared = updatedDashboard.isShared;
        this.theme = updatedDashboard.theme;
        this.keepScreenOn = updatedDashboard.keepScreenOn;
        this.isAppConnectedOn = updatedDashboard.isAppConnectedOn;
        this.isNotificationsOff = updatedDashboard.isNotificationsOff;

        Notification newNotification = updatedDashboard.getWidgetByType(Notification.class);
        if (newNotification != null) {
            Notification oldNotification = this.getWidgetByType(Notification.class);
            if (oldNotification != null) {
                newNotification.iOSTokens = oldNotification.iOSTokens;
                newNotification.androidTokens = oldNotification.androidTokens;
            }
        }

        this.widgets = updatedDashboard.widgets;
        cleanPinStorage(widgets);
    }

    public void updateFaceFields(DashBoard parent) {
        this.name = parent.name;
        this.isShared = parent.isShared;
        this.theme = parent.theme;
        this.keepScreenOn = parent.keepScreenOn;
        this.isAppConnectedOn = parent.isAppConnectedOn;
        this.isNotificationsOff = parent.isNotificationsOff;
        this.tags = copyTags(parent.tags);
        //do not update devices by purpose
        //this.devices = parent.devices;
        this.widgets = copyWidgetsAndPreservePrevValues(this.widgets, parent.widgets);
        //export app specific requirement
        for (Widget widget : widgets) {
            widget.isDefaultColor = false;
        }
    }

    private static Widget[] copyWidgetsAndPreservePrevValues(Widget[] oldWidgets, Widget[] newWidgets) {
        ArrayList<Widget> copy = new ArrayList<>(newWidgets.length);
        for (Widget newWidget : newWidgets) {
            Widget oldWidget = getWidgetById(oldWidgets, newWidget.id);

            Widget copyWidget = newWidget.copy();

            if (oldWidget != null) {
                copyWidget.updateValue(oldWidget);
            }
            copy.add(copyWidget);
        }

        return copy.toArray(new Widget[newWidgets.length]);
    }

    private Tag[] copyTags(Tag[] tagsToCopy) {
        if (tagsToCopy.length == 0) {
            return tagsToCopy;
        }
        Tag[] copy = new Tag[tagsToCopy.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = tagsToCopy[i].copy();
        }
        return copy;
    }

    //removes devices that has no widgets assigned to
    //probably those devices were added via device tiles widget
    public void removeDevicesProvisionedFromDeviceTiles() {
        List<Device> list = new ArrayList<>(Arrays.asList(this.devices));
        list.removeIf(device -> !hasWidgetsByDeviceId(device.id));
        this.devices = list.toArray(new Device[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashBoard dashBoard = (DashBoard) o;
        return id == dashBoard.id &&
                Objects.equals(name, dashBoard.name) &&
                Arrays.equals(widgets, dashBoard.widgets);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(id, name);
        result = 31 * result + Arrays.hashCode(widgets);
        return result;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

}
