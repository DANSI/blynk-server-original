package cc.blynk.utils;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.09.16.
 */
public class DateTimeUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

}
