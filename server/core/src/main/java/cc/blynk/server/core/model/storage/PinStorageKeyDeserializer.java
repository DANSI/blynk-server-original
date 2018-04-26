package cc.blynk.server.core.model.storage;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.11.16.
 */
public class PinStorageKeyDeserializer extends KeyDeserializer {

    @Override
    public PinStorageKey deserializeKey(String key, DeserializationContext ctx) {
        //parsing "123-v24"
        var split = StringUtils.split3(StringUtils.DEVICE_SEPARATOR, key);

        var deviceId = Integer.parseInt(split[0]);
        var pinType = PinType.getPinType(split[1].charAt(0));
        byte pin = 0;
        try {
            pin = Byte.parseByte(split[1].substring(1, split[1].length()));
        } catch (NumberFormatException e) {
            //special case for outdated data format.
            return new PinStorageKey(deviceId, pinType, pin);
        }
        if (split.length == 3) {
            var widgetProperty = WidgetProperty.getProperty(split[2]);
            if (widgetProperty == null) {
                widgetProperty = WidgetProperty.LABEL;
            }
            return new PinPropertyStorageKey(deviceId, pinType, pin, widgetProperty);
        } else {
            return new PinStorageKey(deviceId, pinType, pin);
        }
    }
}
