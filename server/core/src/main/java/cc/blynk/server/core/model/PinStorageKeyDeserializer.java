package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.utils.ParseUtil;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.11.16.
 */
public class PinStorageKeyDeserializer extends KeyDeserializer {

    @Override
    public PinStorageKey deserializeKey(String key, DeserializationContext ctx) throws IOException {
        //parsing "123-v24"
        String[] split = key.split("-");

        int deviceId = ParseUtil.parseInt(split[0]);
        PinType pinType = PinType.getPinType(split[1].charAt(0));
        byte pin = ParseUtil.parseByte(split[1].substring(1, split[1].length()));
        if (split.length == 3) {
            return new PinPropertyStorageKey(deviceId, pinType, pin, split[2]);
        } else {
            return new PinStorageKey(deviceId, pinType, pin);
        }
    }
}
