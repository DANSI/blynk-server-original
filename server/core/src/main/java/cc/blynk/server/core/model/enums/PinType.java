package cc.blynk.server.core.model.enums;

/**
 * User: ddumanskiy
 * Date: 10.12.13
 * Time: 10:15
 */
public enum PinType {

    DIGITAL('d'),
    VIRTUAL('v'),
    ANALOG('a');

    public final char pintTypeChar;
    public final String pinTypeString;

    PinType(char pinType) {
        this.pintTypeChar = pinType;
        this.pinTypeString = String.valueOf(pinType);
    }

    public static PinType getPinType(char pinTypeChar) {
        switch (pinTypeChar) {
            case 'a' :
            case 'A' :
                return ANALOG;
            case 'v' :
            case 'V' :
                return VIRTUAL;
            case 'd' :
            case 'D' :
                return DIGITAL;
            default:
                //NumberFormatException is used for parsing errors
                throw new NumberFormatException("Invalid pin type.");
        }
    }
}
