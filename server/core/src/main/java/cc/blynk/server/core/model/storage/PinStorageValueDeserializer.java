package cc.blynk.server.core.model.storage;

import cc.blynk.server.core.model.storage.value.MultiPinStorageValue;
import cc.blynk.server.core.model.storage.value.MultiPinStorageValueType;
import cc.blynk.server.core.model.storage.value.SinglePinStorageValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_STRING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.04.18.
 */
public class PinStorageValueDeserializer extends JsonDeserializer {

    private static final Logger log = LogManager.getLogger(PinStorageValueDeserializer.class);

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            JsonToken jsonToken = p.currentToken();

            if (jsonToken == VALUE_STRING) {
                return new SinglePinStorageValue(p.getValueAsString());
            }

            if (jsonToken == START_OBJECT) {
                JsonNode multiValueNode = p.getCodec().readTree(p);
                JsonNode type = multiValueNode.get("type");
                if (type != null) {
                    MultiPinStorageValue multiPinStorageValue =
                            new MultiPinStorageValue(MultiPinStorageValueType.valueOf(type.textValue()));

                    JsonNode values = multiValueNode.get("values");
                    if (values != null) {
                        if (values.isArray()) {
                            for (var objNode : values) {
                                multiPinStorageValue.values.add(objNode.textValue());
                            }
                        }
                    }
                    return multiPinStorageValue;
                }
            }
        } catch (Exception e) {
            log.error("Error reading pin storage value.", e);
        }

        return null;
    }
}
