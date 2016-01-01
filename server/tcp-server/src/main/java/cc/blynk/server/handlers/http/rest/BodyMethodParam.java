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

    private String expectedContentType;

    public BodyMethodParam(String name, Class<?> type, String expectedContentType) {
        super(name, type);
        this.expectedContentType = expectedContentType;
    }

    @Override
    Object get(URIDecoder uriDecoder) {
        if (uriDecoder.contentType == null || !uriDecoder.contentType.contains(expectedContentType)) {
            throw new RuntimeException("Unexpected content type. Expecting " + expectedContentType + " but got " + uriDecoder.contentType);
        }
        if (expectedContentType.equals(MediaType.APPLICATION_JSON)) {
            try {
                String data = uriDecoder.bodyData.toString(CharsetUtil.UTF_8);
                if ("".equals(data)) {
                    return null;
                }
                return JsonParser.mapper.readValue(data, type);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing body param.", e);
            }
        } else {
            return uriDecoder.bodyData.toString(CharsetUtil.UTF_8);
        }
    }

}
