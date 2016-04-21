package cc.blynk.server.handlers.http.rest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class RequestHeaderParam extends MethodParam {

    public RequestHeaderParam(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    Object get(URIDecoder uriDecoder) {
        String header = uriDecoder.headers.get(name);
        if (header == null) {
            return null;
        }
        return header;
    }

}
