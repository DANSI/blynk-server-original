package cc.blynk.core.http.rest.params;

import cc.blynk.core.http.rest.URIDecoder;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class QueryParam extends Param {

    public QueryParam(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    public Object get(URIDecoder uriDecoder) {
        List<String> params = uriDecoder.parameters().get(name);
        if (params == null) {
            return null;
        }

        //todo finish. now accepts only strings
        if (type == List.class) {
            return params;
        }

        return convertTo(params.get(0));
    }

}
