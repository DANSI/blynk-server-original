package cc.blynk.server.core.model;

import cc.blynk.utils.JsonParser;
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
        return JsonParser.parsePinStorageKeyFromString(key);
    }
}
