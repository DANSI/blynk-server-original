package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.storage.value.PinStorageValue;
import cc.blynk.server.core.model.storage.value.SinglePinStorageValue;
import cc.blynk.server.core.model.widgets.controls.Button;
import cc.blynk.server.core.model.widgets.controls.LinkButton;
import cc.blynk.server.core.model.widgets.controls.NumberInput;
import cc.blynk.server.core.model.widgets.controls.QR;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.server.core.model.widgets.controls.SegmentedControl;
import cc.blynk.server.core.model.widgets.controls.Slider;
import cc.blynk.server.core.model.widgets.controls.Step;
import cc.blynk.server.core.model.widgets.controls.StyledButton;
import cc.blynk.server.core.model.widgets.controls.Switch;
import cc.blynk.server.core.model.widgets.controls.Terminal;
import cc.blynk.server.core.model.widgets.controls.TextInput;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.controls.TwoAxisJoystick;
import cc.blynk.server.core.model.widgets.controls.VerticalSlider;
import cc.blynk.server.core.model.widgets.controls.VerticalStep;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.SMS;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.others.Bluetooth;
import cc.blynk.server.core.model.widgets.others.BluetoothSerial;
import cc.blynk.server.core.model.widgets.others.Bridge;
import cc.blynk.server.core.model.widgets.others.Player;
import cc.blynk.server.core.model.widgets.others.TextWidget;
import cc.blynk.server.core.model.widgets.others.Video;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.rtc.RTC;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.model.widgets.outputs.Gauge;
import cc.blynk.server.core.model.widgets.outputs.LCD;
import cc.blynk.server.core.model.widgets.outputs.LED;
import cc.blynk.server.core.model.widgets.outputs.LabeledValueDisplay;
import cc.blynk.server.core.model.widgets.outputs.LevelDisplay;
import cc.blynk.server.core.model.widgets.outputs.Map;
import cc.blynk.server.core.model.widgets.outputs.ValueDisplay;
import cc.blynk.server.core.model.widgets.outputs.VerticalLevelDisplay;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.sensors.Accelerometer;
import cc.blynk.server.core.model.widgets.sensors.Barometer;
import cc.blynk.server.core.model.widgets.sensors.GPSStreaming;
import cc.blynk.server.core.model.widgets.sensors.GPSTrigger;
import cc.blynk.server.core.model.widgets.sensors.Gravity;
import cc.blynk.server.core.model.widgets.sensors.Humidity;
import cc.blynk.server.core.model.widgets.sensors.Light;
import cc.blynk.server.core.model.widgets.sensors.Proximity;
import cc.blynk.server.core.model.widgets.sensors.Temperature;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.Menu;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import cc.blynk.server.core.model.widgets.ui.TimeInput;
import cc.blynk.server.core.model.widgets.ui.image.Image;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.table.Table;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.utils.ByteUtils;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.IOException;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:08
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        defaultImpl = Button.class)
@JsonSubTypes({

        //controls
        @JsonSubTypes.Type(value = Button.class, name = "BUTTON"),
        @JsonSubTypes.Type(value = StyledButton.class, name = "STYLED_BUTTON"),
        @JsonSubTypes.Type(value = LinkButton.class, name = "LINK_BUTTON"),
        @JsonSubTypes.Type(value = TextInput.class, name = "TEXT_INPUT"),
        @JsonSubTypes.Type(value = NumberInput.class, name = "NUMBER_INPUT"),
        @JsonSubTypes.Type(value = Slider.class, name = "SLIDER"),
        @JsonSubTypes.Type(value = VerticalSlider.class, name = "VERTICAL_SLIDER"),
        @JsonSubTypes.Type(value = RGB.class, name = "RGB"),
        @JsonSubTypes.Type(value = Timer.class, name = "TIMER"),
        @JsonSubTypes.Type(value = TwoAxisJoystick.class, name = "TWO_AXIS_JOYSTICK"),
        @JsonSubTypes.Type(value = Terminal.class, name = "TERMINAL"),
        @JsonSubTypes.Type(value = Step.class, name = "STEP"),
        @JsonSubTypes.Type(value = VerticalStep.class, name = "VERTICAL_STEP"),
        @JsonSubTypes.Type(value = QR.class, name = "QR"),
        @JsonSubTypes.Type(value = TimeInput.class, name = "TIME_INPUT"),
        @JsonSubTypes.Type(value = SegmentedControl.class, name = "SEGMENTED_CONTROL"),
        @JsonSubTypes.Type(value = Switch.class, name = "SWITCH"),

        //outputs
        @JsonSubTypes.Type(value = LED.class, name = "LED"),
        @JsonSubTypes.Type(value = ValueDisplay.class, name = "DIGIT4_DISPLAY"),
        @JsonSubTypes.Type(value = LabeledValueDisplay.class, name = "LABELED_VALUE_DISPLAY"),
        @JsonSubTypes.Type(value = Gauge.class, name = "GAUGE"),
        @JsonSubTypes.Type(value = LCD.class, name = "LCD"),
        @JsonSubTypes.Type(value = LevelDisplay.class, name = "LEVEL_DISPLAY"),
        @JsonSubTypes.Type(value = VerticalLevelDisplay.class, name = "VERTICAL_LEVEL_DISPLAY"),
        @JsonSubTypes.Type(value = Video.class, name = "VIDEO"),
        @JsonSubTypes.Type(value = Superchart.class, name = "ENHANCED_GRAPH"),

        //sensors
        @JsonSubTypes.Type(value = GPSTrigger.class, name = "GPS_TRIGGER"),
        @JsonSubTypes.Type(value = GPSStreaming.class, name = "GPS_STREAMING"),
        @JsonSubTypes.Type(value = Light.class, name = "LIGHT"),
        @JsonSubTypes.Type(value = Proximity.class, name = "PROXIMITY"),
        @JsonSubTypes.Type(value = Temperature.class, name = "TEMPERATURE"),
        @JsonSubTypes.Type(value = Accelerometer.class, name = "ACCELEROMETER"),
        @JsonSubTypes.Type(value = Gravity.class, name = "GRAVITY"),
        @JsonSubTypes.Type(value = Barometer.class, name = "BAROMETER"),
        @JsonSubTypes.Type(value = Humidity.class, name = "HUMIDITY"),

        //notifications
        @JsonSubTypes.Type(value = Twitter.class, name = "TWITTER"),
        @JsonSubTypes.Type(value = Mail.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = Notification.class, name = "NOTIFICATION"),
        @JsonSubTypes.Type(value = SMS.class, name = "SMS"),

        //interface
        @JsonSubTypes.Type(value = Menu.class, name = "MENU"),
        @JsonSubTypes.Type(value = Tabs.class, name = "TABS"),
        @JsonSubTypes.Type(value = Player.class, name = "PLAYER"),
        @JsonSubTypes.Type(value = Table.class, name = "TABLE"),
        @JsonSubTypes.Type(value = Image.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = ReportingWidget.class, name = "REPORT"),

        //others
        @JsonSubTypes.Type(value = RTC.class, name = "RTC"),
        @JsonSubTypes.Type(value = Bridge.class, name = "BRIDGE"),
        @JsonSubTypes.Type(value = Bluetooth.class, name = "BLUETOOTH"),
        @JsonSubTypes.Type(value = BluetoothSerial.class, name = "BLUETOOTH_SERIAL"),
        @JsonSubTypes.Type(value = Eventor.class, name = "EVENTOR"),
        @JsonSubTypes.Type(value = Map.class, name = "MAP"),
        @JsonSubTypes.Type(value = DeviceSelector.class, name = "DEVICE_SELECTOR"),
        @JsonSubTypes.Type(value = DeviceTiles.class, name = "DEVICE_TILES"),
        @JsonSubTypes.Type(value = TextWidget.class, name = "TEXT"),

        @JsonSubTypes.Type(value = WebHook.class, name = "WEBHOOK")

})
public abstract class Widget implements CopyObject<Widget> {

    public long id;

    public int x;

    public int y;

    public volatile int color;

    public int width;

    public int height;

    public int tabId = 0;

    public volatile String label;

    public boolean isDefaultColor;

    public abstract PinMode getModeType();

    public abstract int getPrice();

    public abstract void updateValue(Widget oldWidget);

    public abstract void erase();

    /**
     * WARNING: this method has one exclusion for DeviceTiles, as
     * Device for Tiles not assigned directly, but assigned via provisioning
     */
    public abstract boolean isAssignedToDevice(int deviceId);

    protected void append(StringBuilder sb, short pin, PinType pinType) {
        if (pin != DataStream.NO_PIN && pinType != PinType.VIRTUAL) {
            PinMode pinMode = getModeType();
            if (pinMode != null) {
                sb.append(BODY_SEPARATOR)
                        .append(pin)
                        .append(BODY_SEPARATOR)
                        .append(pinMode);
            }
        }
    }

    public boolean updateIfSame(int deviceId, short pin, PinType type, String value) {
        return false;
    }

    public boolean isSame(int deviceId, short pin, PinType type) {
        return false;
    }

    public String getJsonValue() {
        return null;
    }

    /**
     * This method should be overridden by every widget that supports direct pins (analog, digital) control
     */
    public void append(StringBuilder sb, int deviceId) {
    }

    //todo this is ugly and not effective. refactor
    @Override
    public Widget copy() {
        String copyWidgetString = JsonParser.toJson(this);
        try {
            return JsonParser.parseWidget(copyWidgetString);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public PinStorageValue getPinStorageValue() {
        return new SinglePinStorageValue();
    }

    public boolean isMultiValueWidget() {
        return false;
    }

    public boolean setProperty(WidgetProperty property, String propertyValue) {
        switch (property) {
            case LABEL :
                this.label = propertyValue;
                return true;
            case COLOR :
                this.color = ByteUtils.parseColor(propertyValue);
                this.isDefaultColor = false;
                return true;
            default:
                return false;
        }
    }
}
