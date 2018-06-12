package cc.blynk.server.core.model.widgets.others.rtc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.09.16.
 */
public class StringToZoneId extends JsonDeserializer<ZoneId> {

    @Override
    public ZoneId deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String zoneString = p.readValueAs(String.class);
        try {
            return ZoneId.of(zoneString);
        } catch (ZoneRulesException e) {
            switch (zoneString) {
                case "Canada/East-Saskatchewan" :
                    return ZoneId.of("America/Regina");
                case "Asia/Hanoi" :
                    return ZoneId.of("Asia/Ho_Chi_Minh");
                default :
                    return ZoneId.of("UTC");
            }
        }
    }

}
