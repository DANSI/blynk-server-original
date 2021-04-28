package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Iterator;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.12.15.
 */
//todo all this should be replaced with 1 Pin field.
public abstract class OnePinWidget extends Widget implements MobileSyncWidget, HardwareSyncWidget {

    public int deviceId;

    public PinType pinType;

    public short pin = -1;

    public boolean pwmMode;

    public boolean rangeMappingOn;

    public float min;

    public float max;

    public volatile String value;

    public static String makeMultiValueHardwareBody(int dashId, int deviceId,
                                                       char pintTypeChar, short pin, Iterator<?> values) {
        StringBuilder sb = new StringBuilder();
        sb.append(dashId).append(DEVICE_SEPARATOR).append(deviceId).append(BODY_SEPARATOR)
          .append(pintTypeChar).append('m').append(BODY_SEPARATOR).append(pin);
        while (values.hasNext()) {
            String value = values.next().toString();
            sb.append(BODY_SEPARATOR).append(value);
        }
        return sb.toString();
    }

    public static String makeHardwareBody(PinType pinType, short pin, String value) {
        return makeHardwareBody(pinType.pintTypeChar, pin, value);
    }

    public static String makeHardwareBody(char pintTypeChar, short pin, String value) {
        return "" + pintTypeChar + 'w' + BODY_SEPARATOR + pin + BODY_SEPARATOR + value;
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
        //do not send SYNC message for widgets assigned to device selector
        //as it will be duplicated later.
        if (isAssignedToDeviceSelector()) {
            return;
        }
        if (targetId == ANY_TARGET || this.deviceId == targetId) {
            String hardBody = makeHardwareBody();
            if (hardBody != null) {
                String body = prependDashIdAndDeviceId(dashId, this.deviceId, hardBody);
                appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body));
            }
        }
    }

    public boolean isAssignedToDeviceSelector() {
        return this.deviceId >= DeviceSelector.DEVICE_SELECTOR_STARTING_ID;
    }

    public boolean isValid() {
        return DataStream.isValid(pin, pinType);
    }

    public boolean isNotValid() {
        return !DataStream.isValid(pin, pinType);
    }

    public String makeHardwareBody() {
        if (isNotValid() || value == null) {
            return null;
        }
        return pwmMode ? makeHardwareBody(PinType.ANALOG, pin, value) : makeHardwareBody(pinType, pin, value);
    }

    @Override
    public boolean updateIfSame(int deviceId, short pin, PinType type, String value) {
        if (isSame(deviceId, pin, type)) {
            this.value = value;
            return true;
        }
        return false;
    }

    @Override
    public void sendHardSync(ChannelHandlerContext ctx, int msgId, int deviceId) {
        if (this.deviceId == deviceId) {
            String body = makeHardwareBody();
            if (body != null) {
                ctx.write(makeUTF8StringMessage(HARDWARE, msgId, body), ctx.voidPromise());
            }
        }
    }

    @Override
    public boolean isSame(int deviceId, short pin, PinType type) {
        return this.deviceId == deviceId && this.pin == pin && (
                (type == this.pinType)
                        || (this.pwmMode && type == PinType.ANALOG)
                        || (type == PinType.DIGITAL && this.pinType == PinType.ANALOG)
        );
    }

    @Override
    public String getJsonValue() {
        if (value == null) {
            return "[]";
        }
        return JsonParser.valueToJsonAsString(value);
    }

    @Override
    public void append(StringBuilder sb, int deviceId) {
        if (this.deviceId == deviceId) {
            append(sb, pin, pinType);
        }
    }

    @Override
    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case MIN :
                //accepting floats as valid, but using int for min/max due to back compatibility
                this.min = Float.parseFloat(propertyValue);
                return true;
            case MAX :
                //accepting floats as valid, but using int for min/max due to back compatibility
                this.max = Float.parseFloat(propertyValue);
                return true;
            default:
                return super.setProperty(property, propertyValue);
        }
    }

    @Override
    public void updateValue(Widget oldWidget) {
        if (oldWidget instanceof OnePinWidget) {
            OnePinWidget onePinWidget = (OnePinWidget) oldWidget;
            if (onePinWidget.value != null) {
                updateIfSame(onePinWidget.deviceId,
                        onePinWidget.pin, onePinWidget.pinType, onePinWidget.value);
            }
        }
    }

    @Override
    public void erase() {
        this.value = null;
    }

    @Override
    public boolean isAssignedToDevice(int deviceId) {
        return this.deviceId == deviceId;
    }
}
