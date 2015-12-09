package cc.blynk.server.handlers.http.admin;

import cc.blynk.server.handlers.http.URIDecoder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public abstract class MethodParam {

    String name;

    Class<?> type;

    public MethodParam(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    abstract Object get(URIDecoder uriDecoder);
}
