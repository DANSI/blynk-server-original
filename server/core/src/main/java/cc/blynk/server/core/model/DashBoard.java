package cc.blynk.server.core.model;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.serialization.View;
import cc.blynk.server.core.model.storage.PinStorageKeyDeserializer;
import cc.blynk.server.core.model.storage.PinStorageValueDeserializer;
import cc.blynk.server.core.model.storage.key.DashPinPropertyStorageKey;
import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.server.core.model.storage.key.PinStorageKey;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.storage.value.SinglePinStorageValue;
import cc.blynk.server.core.model.widgets.DeviceCleaner;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
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
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.ArrayUtil;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

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
    @Deprecated
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
    public PinStorageValue initStorageValueForStorageKey(DashPinStorageKey key) {
        if (!(key instanceof DashPinPropertyStorageKey)) {
            for (Widget widget : widgets) {
                if (widget instanceof OnePinWidget) {
                    OnePinWidget onePinWidget = (OnePinWidget) widget;
                    //pim matches and widget assigned to device selector
                    if (onePinWidget.isAssignedToDeviceSelector() && key.isSame(id, onePinWidget)) {
                        DeviceSelector deviceSelector = getDeviceSelector(onePinWidget.deviceId);
                        if (deviceSelector != null && ArrayUtil.contains(deviceSelector.deviceIds, key.deviceId)) {
                            if (widget.isMultiValueWidget()) {
                                return widget.getPinStorageValue();
                            }
                        }
                    }
                } else if (widget instanceof MultiPinWidget) {
                    MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
                    if (multiPinWidget.isAssignedToDeviceSelector() && key.isSame(id, multiPinWidget)) {
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
                                    if (key.isSame(id, (OnePinWidget) tileWidget)) {
                                        if (tileWidget.isMultiValueWidget()) {
                                            return tileWidget.getPinStorageValue();
                                        }
                                    }
                                } else if (tileWidget instanceof MultiPinWidget) {
                                    if (key.isSame(id, (MultiPinWidget) tileWidget)) {
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

    public boolean hasWidgetsByDeviceId(int deviceId) {
        for (Widget widget : widgets) {
            if (widget.isAssignedToDevice(deviceId)) {
                return true;
            }
        }
        return false;
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

    public static Widget getWidgetById(Widget[] widgets, long id) {
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

    public void eraseWidgetValuesForDevice(int deviceId) {
        for (Widget widget : widgets) {
            if (widget.isAssignedToDevice(deviceId)) {
                widget.erase();
            }
            if (widget instanceof DeviceCleaner) {
                ((DeviceCleaner) widget).deleteDevice(deviceId);
            }
        }
    }

    public void addTimers(TimerWorker timerWorker, UserKey userKey) {
        for (Widget widget : widgets) {
            if (widget instanceof DeviceTiles) {
                timerWorker.add(userKey, (DeviceTiles) widget, id);
            } else if (widget instanceof Timer) {
                timerWorker.add(userKey, (Timer) widget, id, -1L, -1L);
            } else if (widget instanceof Eventor) {
                timerWorker.add(userKey, (Eventor) widget, id);
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
        this.color = settings.color;
        this.isDefaultColor = settings.isDefaultColor;
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
        this.updatedAt = System.currentTimeMillis();
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

    public Widget updateProperty(int deviceId, short pin, WidgetProperty widgetProperty, String propertyValue) {
        Widget widget = null;
        for (Widget dashWidget : widgets) {
            if (dashWidget.isSame(deviceId, pin, PinType.VIRTUAL)) {
                if (dashWidget.setProperty(widgetProperty, propertyValue)) {
                    widget = dashWidget;
                }
            }
        }
        return widget;
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

        if (id != dashBoard.id) {
            return false;
        }
        if (name != null ? !name.equals(dashBoard.name) : dashBoard.name != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(widgets, dashBoard.widgets);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(widgets);
        return result;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

}
