package cc.blynk.server.core.model.storage;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.storage.key.PinPropertyStorageKey;
import cc.blynk.server.core.model.storage.key.PinStorageKey;
import cc.blynk.utils.NumberUtil;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.11.16.
 */
@Deprecated
public class PinStorageKeyDeserializer extends KeyDeserializer {

    @Override
    public PinStorageKey deserializeKey(String key, DeserializationContext ctx) {
        //parsing "123-v24"
        String[] split = StringUtils.split3(StringUtils.DEVICE_SEPARATOR, key);

        int deviceId = Integer.parseInt(split[0]);
        PinType pinType = PinType.getPinType(split[1].charAt(0));
        short pin = 0;
        try {
            pin = NumberUtil.parsePin(split[1].substring(1));
        } catch (NumberFormatException e) {
            //special case for outdated data format.
            return new PinStorageKey(deviceId, pinType, pin);
        }
        if (split.length == 3) {
            WidgetProperty widgetProperty = WidgetProperty.getProperty(split[2]);
            if (widgetProperty == null) {
                widgetProperty = WidgetProperty.LABEL;
            }
            return new PinPropertyStorageKey(deviceId, pinType, pin, widgetProperty);
        } else {
            return new PinStorageKey(deviceId, pinType, pin);
        }
    }
}
