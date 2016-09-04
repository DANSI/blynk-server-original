package cc.blynk.server.core.model.widgets.others.rtc;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.controls.HardwareSyncWidget;
import cc.blynk.utils.DateTimeUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RTC extends OnePinWidget implements HardwareSyncWidget {

    //todo remove in future updates
    @JsonSerialize(using = ZoneOffsetToString.class)
    @JsonDeserialize(using = StringToZoneOffset.class, as = ZoneOffset.class)
    public ZoneOffset timezone;

    @JsonSerialize(using = ZoneIdToString.class)
    @JsonDeserialize(using = StringToZoneId.class, as = ZoneId.class)
    public ZoneId tzName;

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

    private static class ZoneOffsetToString extends JsonSerializer<Object> {

        @Override
        public void serialize(Object value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
            String result = value.toString();
            if (result.equals("Z")) {
                result = "+00:00";
            }
            jsonGenerator.writeObject(result);
        }

    }

    private static class StringToZoneOffset extends JsonDeserializer<ZoneOffset> {

        @Override
        public ZoneOffset deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return ZoneOffset.of(p.readValueAs(String.class));
        }

    }

    @Override
    public boolean isRequiredForSyncAll() {
        return false;
    }

    @Override
    public void send(ChannelHandlerContext ctx, int msgId) {
        final String body = Pin.makeHardwareBody(pinType, pin, getTime());
        ctx.write(makeStringMessage(HARDWARE, msgId, body), ctx.voidPromise());
    }

    private String getTime() {
        ZoneId zone;
        if (tzName != null) {
            zone = tzName;
        } else if (timezone != null) {
            zone = timezone;
        } else {
            zone = DateTimeUtils.UTC;
        }

        LocalDateTime ldt = LocalDateTime.now(zone);
        return String.valueOf(ldt.toEpochSecond(ZoneOffset.UTC));
    }
}
