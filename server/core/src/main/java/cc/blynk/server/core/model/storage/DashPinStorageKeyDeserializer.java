package cc.blynk.server.core.model.storage;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.WidgetProperty;
import cc.blynk.server.core.model.storage.key.DashPinPropertyStorageKey;
import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import cc.blynk.utils.NumberUtil;
import cc.blynk.utils.StringUtils;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.11.18.
 */
public class DashPinStorageKeyDeserializer extends KeyDeserializer {

    @Override
    public DashPinStorageKey deserializeKey(String key, DeserializationContext ctx) {
        //parsing "0-123-v24"
        //or
        //parsing "0-123-v24-property"
        String[] split = key.split(StringUtils.DEVICE_SEPARATOR_STRING);

        int dashId = Integer.parseInt(split[0]);
        int deviceId = Integer.parseInt(split[1]);
        PinType pinType = PinType.getPinType(split[2].charAt(0));
        short pin = NumberUtil.parsePin(split[2].substring(1));

        if (split.length == 4) {
            WidgetProperty widgetProperty = WidgetProperty.getProperty(split[3]);
            if (widgetProperty == null) {
                widgetProperty = WidgetProperty.LABEL;
            }
            return new DashPinPropertyStorageKey(dashId, deviceId, pinType, pin, widgetProperty);
        } else {
            return new DashPinStorageKey(dashId, deviceId, pinType, pin);
        }
    }
}
