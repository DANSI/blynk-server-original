package cc.blynk.server.model.enums;

/**
 * User: ddumanskiy
 * Date: 10.12.13
 * Time: 10:15
 */
public enum PinType {

    NONE('x'),
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
                return ANALOG;
            case 'v' :
                return VIRTUAL;
            case 'd' :
                return DIGITAL;
            default:
                return NONE;
        }
    }

}
