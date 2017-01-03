package cc.blynk.server.core.model;

import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ParseUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static cc.blynk.utils.StringUtils.split3;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:04
 */
public class DashBoard {

    public int id;

    public volatile String name;

    public long createdAt;

    public volatile long updatedAt;

    public volatile Widget[] widgets = {};

    public volatile Device[] devices = {};

    public volatile Tag[] tags = {};

    //todo should be removed after migration
    public String boardType;

    public volatile String theme = "Blynk";

    public volatile boolean keepScreenOn;

    public volatile boolean isAppConnectedOn;

    public volatile boolean isShared;

    public volatile boolean isActive;

    public HardwareInfo hardwareInfo;

    public volatile String sharedToken;

    @JsonDeserialize(keyUsing = PinStorageKeyDeserializer.class)
    public Map<PinStorageKey, String> pinsStorage = new HashMap<>();

    public String getName() {
        return name;
    }

    public void update(int deviceId, String body) {
        update(deviceId, split3(body));
    }

    private void update(int deviceId, String[] splitted) {
        final PinType type = PinType.getPinType(splitted[0].charAt(0));
        final byte pin = ParseUtil.parseByte(splitted[1]);
        update(deviceId, pin, type, splitted[2]);
    }

    public void update(final int deviceId, final byte pin, final PinType type, final String value) {
        boolean hasWidget = false;
        for (Widget widget : widgets) {
            if (widget.updateIfSame(deviceId, pin, type, value)) {
                hasWidget = true;
            }
        }
        //special case. #237 if no widget - storing without widget.
        if (!hasWidget) {
            pinsStorage.put(new PinStorageKey(deviceId, type, pin), value);
        }

        this.updatedAt = System.currentTimeMillis();
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

    public int getWidgetIndexById(long id) {
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
    public int[] getDeviceIdsByTarget(int targetId) {
        Device device = getDeviceById(targetId);
        if (device != null) {
            return new int[] {device.id};
        }

        Tag tag = getTagById(targetId);
        if (tag != null && tag.deviceIds != null && tag.deviceIds.length > 0) {
            return tag.deviceIds;
        }

        //means widget assigned to device selector widget.
        if (targetId >= 200_000) {
            Widget widget = getWidgetById(targetId);
            //todo this check should be removed after fix on iOS
            if (widget instanceof DeviceSelector) {
                return ((DeviceSelector) widget).value;
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

    public Widget getWidgetById(long id) {
        return widgets[getWidgetIndexById(id)];
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
        int sum = 0;
        for (Widget widget : widgets) {
            sum += widget.getPrice();
        }
        return sum;
    }

    public void updateFields(DashBoard updatedDashboard) {
        this.name = updatedDashboard.name;
        this.isShared = updatedDashboard.isShared;
        this.boardType = updatedDashboard.boardType;
        this.keepScreenOn = updatedDashboard.keepScreenOn;
        this.theme = updatedDashboard.theme;

        Notification newNotification = updatedDashboard.getWidgetByType(Notification.class);
        if (newNotification != null) {
            Notification oldNotification = this.getWidgetByType(Notification.class);
            if (oldNotification != null) {
                newNotification.iOSTokens = oldNotification.iOSTokens;
                newNotification.androidTokens = oldNotification.androidTokens;
            }
        }

        this.widgets = updatedDashboard.widgets;
        this.isAppConnectedOn = updatedDashboard.isAppConnectedOn;
        this.updatedAt = System.currentTimeMillis();
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
}
