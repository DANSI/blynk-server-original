package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.controls.*;
import cc.blynk.server.core.model.widgets.inputs.GPS;
import cc.blynk.server.core.model.widgets.notifications.Mail;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.model.widgets.notifications.SMS;
import cc.blynk.server.core.model.widgets.notifications.Twitter;
import cc.blynk.server.core.model.widgets.others.Bluetooth;
import cc.blynk.server.core.model.widgets.others.Bridge;
import cc.blynk.server.core.model.widgets.others.RTC;
import cc.blynk.server.core.model.widgets.outputs.*;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:08
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({

        //controls
        @JsonSubTypes.Type(value = Button.class, name = "BUTTON"),
        @JsonSubTypes.Type(value = Slider.class, name = "SLIDER"),
        @JsonSubTypes.Type(value = RGB.class, name = "RGB"),
        @JsonSubTypes.Type(value = Timer.class, name = "TIMER"),
        @JsonSubTypes.Type(value = OneAxisJoystick.class, name = "ONE_AXIS_JOYSTICK"),
        @JsonSubTypes.Type(value = TwoAxisJoystick.class, name = "TWO_AXIS_JOYSTICK"),
        @JsonSubTypes.Type(value = Terminal.class, name = "TERMINAL"),
        @JsonSubTypes.Type(value = Step.class, name = "STEP"),
        @JsonSubTypes.Type(value = QR.class, name = "QR"),

        //outputs
        @JsonSubTypes.Type(value = LED.class, name = "LED"),
        @JsonSubTypes.Type(value = ValueDisplay.class, name = "DIGIT4_DISPLAY"),
        @JsonSubTypes.Type(value = LabeledValueDisplay.class, name = "LABELED_VALUE_DISPLAY"),
        @JsonSubTypes.Type(value = Gauge.class, name = "GAUGE"),
        @JsonSubTypes.Type(value = LCD.class, name = "LCD"),
        @JsonSubTypes.Type(value = Graph.class, name = "GRAPH"),
        @JsonSubTypes.Type(value = LevelDisplay.class, name = "LEVEL_DISPLAY"),

        //inputs
        @JsonSubTypes.Type(value = GPS.class, name = "GPS"),

        //notifications
        @JsonSubTypes.Type(value = Twitter.class, name = "TWITTER"),
        @JsonSubTypes.Type(value = Mail.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = Notification.class, name = "NOTIFICATION"),
        @JsonSubTypes.Type(value = SMS.class, name = "SMS"),

        //others
        @JsonSubTypes.Type(value = Menu.class, name = "MENU"),
        @JsonSubTypes.Type(value = RTC.class, name = "RTC"),
        @JsonSubTypes.Type(value = Bridge.class, name = "BRIDGE"),
        @JsonSubTypes.Type(value = HistoryGraph.class, name = "LOGGER"),
        @JsonSubTypes.Type(value = Bluetooth.class, name = "BLUETOOTH"),

        //MENU
        @JsonSubTypes.Type(value = Tabs.class, name = "TABS")

})
public abstract class Widget {

    public long id;

    public int x;

    public int y;

    public int color;

    public int width;

    public int height;

    public int tabId = 0;

    public String label;

    public abstract void updateIfSame(byte pin, PinType type, String[] values);

    public abstract boolean isSame(byte pin, PinType type);

    public abstract String getJsonValue();

    public abstract String makeHardwareBody();

    public abstract String getModeType();

    public abstract String getValue(byte pin, PinType type);

    public abstract boolean hasValue(String searchValue);

    public abstract int getPrice();

}
