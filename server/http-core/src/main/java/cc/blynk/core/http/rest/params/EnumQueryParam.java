package cc.blynk.core.http.rest.params;

import cc.blynk.core.http.rest.URIDecoder;
import io.netty.channel.ChannelHandlerContext;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class EnumQueryParam extends Param {

    public EnumQueryParam(Class enumType) {
        super(null, enumType);
        if (!type.isEnum()) {
            throw new RuntimeException("Should be enum.");
        }
    }

    @Override
    public Object get(ChannelHandlerContext ctx, URIDecoder uriDecoder) {
        Map<String, List<String>> params = uriDecoder.parameters();
        for (Object enumValue : type.getEnumConstants()) {
            List<String> paramsValues = params.get(enumValue.toString());
            if (paramsValues != null) {
                return new AbstractMap.SimpleImmutableEntry<>(enumValue, paramsValues.get(0));
            }
        }
        return null;
    }

}
