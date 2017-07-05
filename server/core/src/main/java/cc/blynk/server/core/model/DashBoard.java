package cc.blynk.server.core.model;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.widgets.*;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ParseUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import java.util.*;

import static cc.blynk.server.core.model.widgets.AppSyncWidget.ANY_TARGET;
import static cc.blynk.utils.ArrayUtil.*;

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

    public volatile boolean isShared;

    public volatile boolean isActive;

    public volatile HardwareInfo hardwareInfo;

    public volatile String sharedToken;

    @JsonDeserialize(keyUsing = PinStorageKeyDeserializer.class)
    public Map<PinStorageKey, String> pinsStorage = Collections.emptyMap();

    public void update(final int deviceId, final byte pin, final PinType type, final String value, final long now) {
        boolean hasWidget = false;
        for (Widget widget : widgets) {
            if (widget.updateIfSame(deviceId, pin, type, value)) {
                hasWidget = true;
            }
        }
        //special case. #237 if no widget - storing without widget.
        if (!hasWidget) {
            putPinStorageValue(deviceId, type, pin, value);
        }

        this.updatedAt = now;
    }

    public void putPinPropertyStorageValue(int deviceId, PinType type, byte pin, String property, String value) {
        puntPinStorageValue(new PinPropertyStorageKey(deviceId, type, pin, property), value);
    }

    private void putPinStorageValue(int deviceId, PinType type, byte pin, String value) {
        puntPinStorageValue(new PinStorageKey(deviceId, type, pin), value);
    }

    private void puntPinStorageValue(PinStorageKey key, String value) {
        if (pinsStorage == Collections.EMPTY_MAP) {
            pinsStorage = new HashMap<>();
        }
        pinsStorage.put(key, value);
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
        final PinType type = PinType.getPinType(splitted[0].charAt(0));
        final byte pin = ParseUtil.parseByte(splitted[1]);
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

    public int getWidgetIndexByIdOrThrow(long id) {
        for (int i = 0; i < widgets.length; i++) {
            if (widgets[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Widget with passed id not found.");
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
            Widget widget = getWidgetByIdOrThrow(targetId);
            if (widget instanceof Target) {
                return (Target) widget;
            }
        }

        return null;
    }

    public Device getDeviceById(int id) {
        for (Device device : devices) {
            if (device.id == id) {
                return device;
            }
        }
        return null;
    }

    public Widget getWidgetByIdOrThrow(long id) {
        return widgets[getWidgetIndexByIdOrThrow(id)];
    }

    public Widget getWidgetById(long id) {
        for (Widget widget : widgets) {
            if (widget.id == id) {
                return widget;
            }
        }
        return null;
    }

    public  <T> T getWidgetByType(Class<T> clazz) {
        for (Widget widget : widgets) {
            if (clazz.isInstance(widget)) {
                return clazz.cast(widget);
            }
        }
        return null;
    }

    public String buildPMMessage(int deviceId) {
        StringBuilder sb = new StringBuilder("pm");
        for (Widget widget : widgets) {
            widget.append(sb, deviceId);
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
        for (Widget widget : widgets) {
            if (widget instanceof OnePinWidget) {
                ((OnePinWidget) widget).value = null;
            }
            if (widget instanceof MultiPinWidget) {
                for (Pin pin : ((MultiPinWidget) widget).pins) {
                    if (pin != null) {
                        pin.value = null;
                    }
                }
            }
        }
    }

    public void deleteTimers(TimerWorker timerWorker, UserKey userKey) {
        for (Widget widget : widgets) {
            if (widget instanceof Timer) {
                timerWorker.delete(userKey, (Timer) widget, id);
            } else if (widget instanceof Eventor) {
                timerWorker.delete(userKey, (Eventor) widget, id);
            }
        }
    }

    public void addTimers(TimerWorker timerWorker, UserKey userKey) {
        for (Widget widget : widgets) {
            if (widget instanceof Timer) {
                timerWorker.add(userKey, (Timer) widget, id);
            }
            if (widget instanceof Eventor) {
                timerWorker.add(userKey, (Eventor) widget, id);
            }
        }
    }

    public void cleanPinStorage(Widget widget) {
        if (widget instanceof OnePinWidget) {
            OnePinWidget onePinWidget = (OnePinWidget) widget;
            if (onePinWidget.pinType != null) {
                pinsStorage.remove(new PinStorageKey(onePinWidget.deviceId, onePinWidget.pinType, onePinWidget.pin));
            }
        } else if (widget instanceof MultiPinWidget) {
            MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
            if (multiPinWidget.pins != null) {
                for (Pin pin : multiPinWidget.pins) {
                    if (pin != null && pin.pinType != null) {
                        pinsStorage.remove(new PinStorageKey(multiPinWidget.deviceId, pin.pinType, pin.pin));
                    }
                }
            }
        }
    }

    public void sendSyncs(Channel appChannel, int targetId) {
        for (Widget widget : widgets) {
            if (widget instanceof AppSyncWidget && appChannel.isWritable()) {
                ((AppSyncWidget) widget).sendAppSync(appChannel, id, targetId);
            }
        }

        for (Map.Entry<PinStorageKey, String> entry : pinsStorage.entrySet()) {
            PinStorageKey key = entry.getKey();
            if ((targetId == ANY_TARGET || targetId == key.deviceId) && appChannel.isWritable()) {
                ByteBuf byteBuf = key.makeByteBuf(id, entry.getValue());
                appChannel.write(byteBuf, appChannel.voidPromise());
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
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateFields(DashBoard updatedDashboard) {
        this.name = updatedDashboard.name;
        this.isShared = updatedDashboard.isShared;
        this.theme = updatedDashboard.theme;
        this.keepScreenOn = updatedDashboard.keepScreenOn;
        this.isAppConnectedOn = updatedDashboard.isAppConnectedOn;

        Notification newNotification = updatedDashboard.getWidgetByType(Notification.class);
        if (newNotification != null) {
            Notification oldNotification = this.getWidgetByType(Notification.class);
            if (oldNotification != null) {
                newNotification.iOSTokens = oldNotification.iOSTokens;
                newNotification.androidTokens = oldNotification.androidTokens;
            }
        }

        this.widgets = updatedDashboard.widgets;

        for (Widget widget : widgets) {
            cleanPinStorage(widget);
        }

        this.updatedAt = System.currentTimeMillis();
    }

    public void updateFaceFields(DashBoard parent) {
        this.name = parent.name;
        this.isShared = parent.isShared;
        this.theme = parent.theme;
        this.keepScreenOn = parent.keepScreenOn;
        this.isAppConnectedOn = parent.isAppConnectedOn;
        this.tags = copyTags(parent.tags);
        //do not update devices by purpose
        //this.devices = parent.devices;
        this.widgets = copyWidgets(parent.widgets);
    }

    private Widget[] copyWidgets(Widget[] widgetsToCopy) {
        if (widgetsToCopy.length == 0) {
            return widgetsToCopy;
        }
        ArrayList<Widget> copy = new ArrayList<>(widgetsToCopy.length);
        for (Widget newWidget : widgetsToCopy) {
            Widget oldWidget = getWidgetById(newWidget.id);

            String copyWidgetString = JsonParser.toJson(newWidget);
            Widget copyWidget = JsonParser.parseWidget(copyWidgetString);

            if (oldWidget != null) {
                if (oldWidget instanceof OnePinWidget) {
                    OnePinWidget onePinWidget = (OnePinWidget) oldWidget;
                    if (onePinWidget.value != null) {
                        copyWidget.updateIfSame(oldWidget);
                    }
                } else {
                    copyWidget.updateIfSame(oldWidget);
                }
            }
            copyWidget.isDefaultColor = false;
            copy.add(copyWidget);
        }

        return copy.toArray(new Widget[widgetsToCopy.length]);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DashBoard dashBoard = (DashBoard) o;

        if (id != dashBoard.id) return false;
        if (name != null ? !name.equals(dashBoard.name) : dashBoard.name != null) return false;
        if (!Arrays.equals(widgets, dashBoard.widgets)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (widgets != null ? Arrays.hashCode(widgets) : 0);
        return result;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

    public String toStringRestrictive() {
        return JsonParser.toJsonRestrictiveDashboard(this);
    }
}
