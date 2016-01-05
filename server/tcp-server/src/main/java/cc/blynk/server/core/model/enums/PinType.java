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

    public char pintTypeChar;

    PinType(char pinType) {
        this.pintTypeChar = pinType;
    }

    public static PinType getPingType(char pinTypeChar) {
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
                throw new NumberFormatException("Invalid pin type.");
        }
    }

}
