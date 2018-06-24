package cc.blynk.server.core.model.widgets.others.rtc;

import cc.blynk.utils.DateTimeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.ZoneId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.09.16.
 */
public class StringToZoneId extends JsonDeserializer<ZoneId> {

    public static ZoneId parseZoneId(String zoneString) {
        try {
            return ZoneId.of(zoneString);
        } catch (Exception e) {
            switch (zoneString) {
                case "Canada/East-Saskatchewan" :
                    return DateTimeUtils.AMERICA_REGINA;
                case "Asia/Hanoi" :
                    return DateTimeUtils.ASIA_HO_CHI;
                default :
                    return DateTimeUtils.UTC;
            }
        }
    }

    @Override
    public ZoneId deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String zoneString = p.readValueAs(String.class);
        return parseZoneId(zoneString);
    }

}
