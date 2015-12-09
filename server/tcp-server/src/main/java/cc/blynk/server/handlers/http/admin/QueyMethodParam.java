package cc.blynk.server.handlers.http.admin;

import cc.blynk.server.handlers.http.URIDecoder;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class QueyMethodParam extends MethodParam {

    public QueyMethodParam(String name, Class<?> type) {
        super(name, type);
    }

    //todo better way via method reference?
    //Function<String, Integer> stringToInt = x -> Integer.valueOf(x);
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
    Object get(URIDecoder uriDecoder) {
        List<String> params = uriDecoder.parameters().get(name);
        if (params == null) {
            return null;
        }
        return convertTo(type, params.get(0));
    }

}
