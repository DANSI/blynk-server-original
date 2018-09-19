package cc.blynk.server.application.handlers.main.auth;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.03.16.
 */
public enum OsType {

    ANDROID("android"),
    IOS("iOS"),
    //3d party clients or unknown clients
    OTHER("unknown");

    public final String label;

    OsType(String label) {
        this.label = label;
    }

    public static OsType parse(String type) {
        switch (type.toLowerCase()) {
            case "ios" :
                return IOS;
            case "android" :
                return ANDROID;
            default:
                return OTHER;
        }
    }

}
