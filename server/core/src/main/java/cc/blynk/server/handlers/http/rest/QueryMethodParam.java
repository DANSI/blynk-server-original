package cc.blynk.server.handlers.http.rest;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class QueryMethodParam extends MethodParam {

    public QueryMethodParam(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    Object get(URIDecoder uriDecoder) {
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
