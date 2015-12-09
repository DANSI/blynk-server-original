package cc.blynk.server.handlers.http.admin;

import cc.blynk.server.handlers.http.URIDecoder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class PathMethodParam extends MethodParam {

    public PathMethodParam(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    Object get(URIDecoder uriDecoder) {
        return uriDecoder.pathData.get(name);
    }

}
