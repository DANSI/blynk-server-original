package cc.blynk.server.handlers.http.rest;

import io.netty.util.CharsetUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class FormMethodParam extends MethodParam {

    public FormMethodParam(String name, Class<?> type) {
        super(name, type);
    }

    public static Object convertTo(Class type, String value) {
        if (type == long.class) {
            return Long.valueOf(value);
        }
        if (type == int.class || type == Integer.class) {
            return Integer.valueOf(value);
        }
        if (type == short.class || type == Short.class) {
            return Short.valueOf(value);
        }
        if (type == boolean.class) {
            return Boolean.valueOf(value);
        }
        return value;
    }

    @Override
    //todo this method is not optimal - optimize.
    Object get(URIDecoder uriDecoder) {
        String body = uriDecoder.bodyData.toString(CharsetUtil.UTF_8);

        List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(body, CharsetUtil.UTF_8);
        if (nameValuePairs == null || nameValuePairs.size() == 0) {
            return null;
        }

        for(NameValuePair nameValuePair : nameValuePairs) {
            if (name.equals(nameValuePair.getName())) {
                return convertTo(type, nameValuePair.getValue());
            }
        }

        return null;
    }

}
