package cc.blynk.server.handlers.http.rest;

import cc.blynk.server.utils.JsonParser;
import io.netty.util.CharsetUtil;

import javax.ws.rs.core.MediaType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class BodyMethodParam extends MethodParam {

    private String contentType;

    public BodyMethodParam(String name, Class<?> type, String contentType) {
        super(name, type);
        this.contentType = contentType;
    }

    @Override
    Object get(URIDecoder uriDecoder) {
        if (!uriDecoder.contentType.contains(contentType)) {
            throw new RuntimeException("Unexpected content type for handler. Expecting " + contentType + " but got " + contentType);
        }
        if (contentType.equals(MediaType.APPLICATION_JSON)) {
            try {
                return JsonParser.mapper.readValue(uriDecoder.bodyData.toString(CharsetUtil.UTF_8), type);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing body param.", e);
            }
        } else {
            return uriDecoder.bodyData.toString(CharsetUtil.UTF_8);
        }
    }

}
