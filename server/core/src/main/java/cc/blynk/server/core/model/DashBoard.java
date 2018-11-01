package cc.blynk.server.core.model;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.serialization.View;
import cc.blynk.server.core.model.storage.PinPropertyStorageKey;
import cc.blynk.server.core.model.storage.PinStorageKey;
import cc.blynk.server.core.model.storage.PinStorageKeyDeserializer;
import cc.blynk.server.core.model.storage.PinStorageValue;
import cc.blynk.server.core.model.storage.PinStorageValueDeserializer;
import cc.blynk.server.core.model.storage.SinglePinStorageValue;
import cc.blynk.server.core.model.widgets.DeviceCleaner;
import cc.blynk.server.core.model.widgets.MobileSyncWidget;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.Tile;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static cc.blynk.server.core.model.widgets.MobileSyncWidget.ANY_TARGET;
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
    private static final String DEFAULT_NAME = "New Project";

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

    public volatile boolean widgetBackgroundOn;

    public int color = -1;

    public boolean isDefaultColor = true;

    @JsonView(View.Private.class)
    public volatile String sharedToken;

    @JsonView(View.Private.class)
    @JsonDeserialize(keyUsing = PinStorageKeyDeserializer.class,
                     contentUsing = PinStorageValueDeserializer.class)
    public Map<PinStorageKey, PinStorageValue> pinsStorage = Collections.emptyMap();

    public boolean updateWidgets(int deviceId, short pin, PinType type, String value) {
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

    public String getNameOrDefault() {
        return name == null ? DEFAULT_NAME : name;
    }

    //multi value widgets has always priority over single value widgets.
    //for example, we have 2 widgets on the same pin, one it terminal, another is value display.
    //so for that pin we have to return multivalue storage
    public PinStorageValue initStorageValueForStorageKey(PinStorageKey key) {
        if (!(key instanceof PinPropertyStorageKey)) {
            for (Widget widget : widgets) {
                if (widget instanceof OnePinWidget) {
                    OnePinWidget onePinWidget = (OnePinWidget) widget;
                    //pim matches and widget assigned to device selector
                    if (onePinWidget.isAssignedToDeviceSelector() && key.isSamePin(onePinWidget)) {
                        DeviceSelector deviceSelector = getDeviceSelector(onePinWidget.deviceId);
                        if (deviceSelector != null && ArrayUtil.contains(deviceSelector.deviceIds, key.deviceId)) {
                            if (widget.isMultiValueWidget()) {
                                return widget.getPinStorageValue();
                            }
                        }
                    }
                } else if (widget instanceof MultiPinWidget) {
                    MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
                    if (multiPinWidget.isAssignedToDeviceSelector() && key.isSamePin(multiPinWidget)) {
                        DeviceSelector deviceSelector = getDeviceSelector(multiPinWidget.deviceId);
                        if (deviceSelector != null && ArrayUtil.contains(deviceSelector.deviceIds, key.deviceId)) {
                            if (widget.isMultiValueWidget()) {
                                return widget.getPinStorageValue();
                            }
                        }
                    }
                } else if (widget instanceof DeviceTiles) {
                    DeviceTiles deviceTiles = (DeviceTiles) widget;
                    for (TileTemplate template : deviceTiles.templates) {
                        if (ArrayUtil.contains(template.deviceIds, key.deviceId)) {
                            for (Widget tileWidget : template.widgets) {
                                if (tileWidget instanceof OnePinWidget) {
                                    if (key.isSamePin((OnePinWidget) tileWidget)) {
                                        if (tileWidget.isMultiValueWidget()) {
                                            return tileWidget.getPinStorageValue();
                                        }
                                    }
                                } else if (tileWidget instanceof MultiPinWidget) {
                                    if (key.isSamePin((MultiPinWidget) tileWidget)) {
                                        if (tileWidget.isMultiValueWidget()) {
                                            return tileWidget.getPinStorageValue();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return new SinglePinStorageValue();
    }

    public void activate() {
        isActive = true;
        updatedAt = System.currentTimeMillis();
    }

    public void deactivate() {
        isActive = false;
        updatedAt = System.currentTimeMillis();
    }

    public Widget findWidgetByPin(int deviceId, short pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget.isSame(deviceId, pin, pinType)) {
                return widget;
            }
        }
        return null;
    }

    public WebHook findWebhookByPin(int deviceId, short pin, PinType pinType) {
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

    public void deleteTag(int tagId) {
        int existingTagIndex = getTagIndexByIdOrThrow(tagId);
        this.tags = ArrayUtil.remove(this.tags, existingTagIndex, Tag.class);
    }

    public void addTag(Tag newTag) {
        this.tags = ArrayUtil.add(tags, newTag, Tag.class);
    }

    public int getTagIndexByIdOrThrow(int id) {
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

    private int getDeviceIndexByIdOrThrow(int id) {
        for (int i = 0; i < devices.length; i++) {
            if (devices[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Device with passed id not found.");
    }

    public boolean hasWidgetsByDeviceId(int deviceId) {
        for (Widget widget : widgets) {
            if (widget.isAssignedToDevice(deviceId)) {
                return true;
            }
        }
        return false;
    }

    public Device getDeviceById(int id) {
        for (Device device : devices) {
            if (device.id == id) {
                return device;
            }
        }
        return null;
    }

    public DeviceSelector getDeviceSelector(long targetId) {
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

    public Notification getNotificationWidget() {
        return getWidgetByType(Notification.class);
    }

    public Eventor getEventorWidget() {
        return getWidgetByType(Eventor.class);
    }

    public Twitter getTwitterWidget() {
        return getWidgetByType(Twitter.class);
    }

    public Mail getMailWidget() {
        return getWidgetByType(Mail.class);
    }

    public ReportingWidget getReportingWidget() {
        return getWidgetByType(ReportingWidget.class);
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

    public void eraseWidgetValues() {
        for (Widget widget : widgets) {
            widget.erase();
        }
    }

    private void erasePinStorageForDevice(int deviceId) {
        pinsStorage.entrySet().removeIf(entry -> entry.getKey().deviceId == deviceId);
    }

    private void eraseWidgetValuesForDevice(int deviceId) {
        for (Widget widget : widgets) {
            if (widget.isAssignedToDevice(deviceId)) {
                widget.erase();
            }
            if (widget instanceof DeviceCleaner) {
                ((DeviceCleaner) widget).deleteDevice(deviceId);
            }
        }
    }

    public void deleteDeviceFromTags(int deviceId) {
        for (Tag tag : tags) {
            tag.deleteDevice(deviceId);
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

    public void cleanPinStorage(Widget widget, boolean removeTemplates) {
        cleanPinStorageInternalWithoutUpdatedAt(widget, true, removeTemplates);
        this.updatedAt = System.currentTimeMillis();
    }

    private void cleanPinStorage(DeviceTiles deviceTiles, boolean removeProperties) {
        for (Tile tile : deviceTiles.tiles) {
            if (tile != null && tile.isValidDataStream()) {
                DataStream dataStream = tile.dataStream;
                pinsStorage.remove(new PinStorageKey(tile.deviceId, dataStream.pinType, dataStream.pin));
                if (removeProperties) {
                    for (WidgetProperty widgetProperty : WidgetProperty.values()) {
                        pinsStorage.remove(new PinPropertyStorageKey(tile.deviceId,
                                dataStream.pinType, dataStream.pin, widgetProperty));
                    }
                }
            }
        }
    }

    public void cleanPinStorageForTileTemplate(TileTemplate tileTemplate, boolean removeProperties) {
        for (int deviceId : tileTemplate.deviceIds) {
            for (Widget widget : tileTemplate.widgets) {
                if (widget instanceof OnePinWidget) {
                    OnePinWidget onePinWidget = (OnePinWidget) widget;
                    cleanPinStorage(this, onePinWidget, deviceId, removeProperties);
                } else if (widget instanceof MultiPinWidget) {
                    MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
                    cleanPinStorage(this, multiPinWidget, deviceId, removeProperties);
                }
            }
        }
    }

    private static void cleanPinStorage(DashBoard dash,
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

    private static void cleanPinStorage(DashBoard dash,
                                        OnePinWidget onePinWidget, int targetId, boolean removeProperties) {
        if (onePinWidget.isValid()) {
            removePinStorageValue(dash, targetId == -1 ? onePinWidget.deviceId : targetId,
                    onePinWidget.pinType, onePinWidget.pin, removeProperties);
        }
    }

    private static void removePinStorageValue(DashBoard dash,
                                              int targetId, PinType pinType, short pin, boolean removeProperties) {
        Target target;
        if (targetId < Tag.START_TAG_ID) {
            target = dash.getDeviceById(targetId);
        } else if (targetId < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
            target = dash.getTagById(targetId);
        } else {
            //means widget assigned to device selector widget.
            target = dash.getDeviceSelector(targetId);
        }
        if (target != null) {
            for (int deviceId : target.getAssignedDeviceIds()) {
                dash.pinsStorage.remove(new PinStorageKey(deviceId, pinType, pin));
                if (removeProperties) {
                    for (WidgetProperty widgetProperty : WidgetProperty.values()) {
                        dash.pinsStorage.remove(new PinPropertyStorageKey(deviceId, pinType, pin, widgetProperty));
                    }
                }
            }
        }
    }

    public void sendAppSyncs(Channel appChannel, int targetId, boolean useNewFormat) {
        for (Widget widget : widgets) {
            if (widget instanceof MobileSyncWidget && appChannel.isWritable()) {
                ((MobileSyncWidget) widget).sendAppSync(appChannel, id, targetId, useNewFormat);
            }
        }

        for (Map.Entry<PinStorageKey, PinStorageValue> entry : pinsStorage.entrySet()) {
            PinStorageKey key = entry.getKey();
            if ((targetId == ANY_TARGET || targetId == key.deviceId) && appChannel.isWritable()) {
                PinStorageValue pinStorageValue = entry.getValue();
                pinStorageValue.sendAppSync(appChannel, id, key, useNewFormat);
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
        this.widgetBackgroundOn = settings.widgetBackgroundOn;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateFields(DashBoard updatedDashboard) {
        this.name = updatedDashboard.name;
        this.isShared = updatedDashboard.isShared;
        this.theme = updatedDashboard.theme;
        this.keepScreenOn = updatedDashboard.keepScreenOn;
        this.isAppConnectedOn = updatedDashboard.isAppConnectedOn;
        this.isNotificationsOff = updatedDashboard.isNotificationsOff;
        this.widgetBackgroundOn = updatedDashboard.widgetBackgroundOn;
        this.color = updatedDashboard.color;
        this.isDefaultColor = updatedDashboard.isDefaultColor;

        Notification newNotification = updatedDashboard.getNotificationWidget();
        if (newNotification != null) {
            Notification oldNotification = this.getNotificationWidget();
            if (oldNotification != null) {
                newNotification.iOSTokens = oldNotification.iOSTokens;
                newNotification.androidTokens = oldNotification.androidTokens;
            }
        }

        this.widgets = updatedDashboard.widgets;
    }

    public void updateFaceFields(DashBoard parent) {
        this.name = parent.name;
        this.isShared = parent.isShared;
        this.theme = parent.theme;
        this.keepScreenOn = parent.keepScreenOn;
        this.isAppConnectedOn = parent.isAppConnectedOn;
        this.isNotificationsOff = parent.isNotificationsOff;
        this.widgetBackgroundOn = parent.widgetBackgroundOn;
        this.color = parent.color;
        this.isDefaultColor = parent.isDefaultColor;
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

            //for now erasing only for this types, not sure about DeviceTiles
            if (copyWidget instanceof OnePinWidget
                    || copyWidget instanceof MultiPinWidget
                    || copyWidget instanceof ReportingWidget) {
                copyWidget.erase();
            }

            if (oldWidget != null) {
                copyWidget.updateValue(oldWidget);
            }
            copy.add(copyWidget);
        }

        return copy.toArray(new Widget[newWidgets.length]);
    }

    public void cleanPinStorage(Widget[] widgets,
                                boolean removeProperties, boolean eraseTemplates) {
        for (Widget widget : widgets) {
            cleanPinStorageInternalWithoutUpdatedAt(widget, removeProperties, eraseTemplates);
        }
        this.updatedAt = System.currentTimeMillis();
    }

    private void cleanPinStorageInternalWithoutUpdatedAt(Widget widget,
                                                         boolean removeProperties, boolean eraseTemplates) {
        if (widget instanceof OnePinWidget) {
            OnePinWidget onePinWidget = (OnePinWidget) widget;
            cleanPinStorage(this, onePinWidget, -1, removeProperties);
        } else if (widget instanceof MultiPinWidget) {
            MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
            cleanPinStorage(this, multiPinWidget, -1, removeProperties);
        } else if (widget instanceof DeviceTiles) {
            DeviceTiles deviceTiles = (DeviceTiles) widget;
            cleanPinStorage(deviceTiles, removeProperties);
            if (eraseTemplates) {
                cleanPinStorageForTemplate(deviceTiles, removeProperties);
            }
        }
    }

    private void cleanPinStorageForTemplate(DeviceTiles deviceTiles, boolean removeProperties) {
        for (TileTemplate tileTemplate : deviceTiles.templates) {
            cleanPinStorageForTileTemplate(tileTemplate, removeProperties);
        }
    }

    public Device deleteDevice(int deviceId) {
        int existingDeviceIndex = getDeviceIndexByIdOrThrow(deviceId);
        Device deviceToRemove = this.devices[existingDeviceIndex];
        this.devices = ArrayUtil.remove(this.devices, existingDeviceIndex, Device.class);
        eraseWidgetValuesForDevice(deviceId);
        erasePinStorageForDevice(deviceId);
        this.updatedAt = System.currentTimeMillis();
        return deviceToRemove;
    }

    public void addDevice(Device device) {
        this.devices = ArrayUtil.add(this.devices, device, Device.class);
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DashBoard dashBoard = (DashBoard) o;
        return id == dashBoard.id
                && Objects.equals(name, dashBoard.name)
                && Arrays.equals(widgets, dashBoard.widgets);
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
