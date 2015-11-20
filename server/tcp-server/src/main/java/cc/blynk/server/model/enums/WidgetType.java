package cc.blynk.server.model.enums;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:12
 */
public enum WidgetType {

    //controls
    BUTTON,
    SLIDER,
    VERTICAL_SLIDER,
    KNOB,
    TIMER,
    ROTARY_KNOB,
    RGB,
    TWO_WAY_ARROW,
    FOUR_WAY_ARROW,
    ONE_AXIS_JOYSTICK,
    TWO_AXIS_JOYSTICK,
    GAMEPAD,
    KEYPAD,

    //outputs
    LED,
    DIGIT4_DISPLAY, //same as NUMERICAL_DISPLAY
    GAUGE,
    LCD_DISPLAY,
    GRAPH,
    LEVEL_DISPLAY,
    TERMINAL,

    //inputs
    MICROPHONE,
    GYROSCOPE,
    ACCELEROMETER,
    GPS,

    //notifications
    TWITTER,
    EMAIL,
    NOTIFICATION,

    //other
    LOGGER,
    SD_CARD,
    EVENTOR,
    RCT,
    BRIDGE,
    BLUETOOTH

}
