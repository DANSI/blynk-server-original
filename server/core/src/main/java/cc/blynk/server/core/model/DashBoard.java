package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ParseUtil;

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

    public String name;

    public long createdAt;

    public long updatedAt;

    public Widget[] widgets = {};

    public String boardType;

    public String theme = "Blynk";

    public boolean keepScreenOn;

    public boolean isShared;

    public boolean isActive;

    public HardwareInfo hardwareInfo;

    //used only for business UI
    public String token;

    public Map<String, Object> metadata = new HashMap<>();

    public Map<String, String> storagePins = new HashMap<>();

    public String getName() {
        return name;
    }

    public void update(String body) {
        update(split3(body));
    }

    private void update(String[] splitted) {
        final PinType type = PinType.getPinType(splitted[0].charAt(0));
        final byte pin = ParseUtil.parseByte(splitted[1]);
        update(pin, type, splitted[2]);
    }

    public void update(final byte pin, final PinType type, final String value) {
        boolean hasWidget = false;
        for (Widget widget : widgets) {
            if (widget.updateIfSame(pin, type, value)) {
                hasWidget = true;
            }
        }
        //special case. #237 if no widget - storing without widget.
        if (!hasWidget) {
            storagePins.put(String.valueOf(type.pintTypeChar) + pin, value);
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

    public Widget findWidgetByPin(String[] splitted) {
        final PinType type = PinType.getPinType(splitted[0].charAt(0));
        final byte pin = ParseUtil.parseByte(splitted[1]);
        return findWidgetByPin(pin, type);
    }

    public Widget findWidgetByPin(byte pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget.isSame(pin, pinType)) {
                return widget;
            }
        }
        return null;
    }

    public WebHook findWebhookByPin(byte pin, PinType pinType) {
        for (Widget widget : widgets) {
            if (widget instanceof WebHook) {
                WebHook webHook = (WebHook) widget;
                if (webHook.isSameWebHook(pin, pinType)) {
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

    public String buildPMMessage() {
        StringBuilder sb = new StringBuilder("pm");
        for (Widget widget : widgets) {
            widget.append(sb);
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
