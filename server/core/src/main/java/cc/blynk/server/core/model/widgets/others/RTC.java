package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.widgets.OnePinWidget;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.time.ZoneOffset;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RTC extends OnePinWidget {

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneOffset.class)
    public ZoneOffset timezone;

    @Override
    public String getModeType() {
        return null;
    }

    @Override
    public String makeHardwareBody() {
        return null;
    }

    @Override
    public int getPrice() {
        return 100;
    }

    private static class ZoneIdToString extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
            jsonGenerator.writeObject(value.toString());
        }
    }

    private static class StringToZoneId extends JsonDeserializer<ZoneOffset> {

        @Override
        public ZoneOffset deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return ZoneOffset.of(p.readValueAs(String.class));
        }
    }
}
