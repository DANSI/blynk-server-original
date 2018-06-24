package cc.blynk.utils;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.09.16.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final ZoneId AMERICA_REGINA = ZoneId.of("America/Regina");
    public static final ZoneId ASIA_HO_CHI = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

}
