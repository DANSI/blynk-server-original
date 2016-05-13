package cc.blynk.server.handlers.http.rest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class ContextParam extends MethodParam {

    public ContextParam(Class<?> type) {
        super(null, type);
    }

    @Override
    Object get(URIDecoder uriDecoder) {
        return null;
    }

}
