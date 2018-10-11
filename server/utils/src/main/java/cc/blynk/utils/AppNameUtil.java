package cc.blynk.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 5/17/16.
 */
public final class AppNameUtil {

    private AppNameUtil() {
    }

    public static final String BLYNK = "Blynk";
    public static final String FACEBOOK = "facebook";

    public static String generateAppId() {
        return AppNameUtil.BLYNK.toLowerCase() + StringUtils.randomString(8);
    }

}
