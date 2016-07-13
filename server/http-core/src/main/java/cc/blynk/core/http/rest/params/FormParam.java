package cc.blynk.core.http.rest.params;

import cc.blynk.core.http.rest.URIDecoder;
import cc.blynk.utils.ReflectionUtil;
import io.netty.util.CharsetUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class FormParam extends Param {

    public FormParam(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    //todo this method is not optimal - optimize.
    public Object get(URIDecoder uriDecoder) {
        String body = uriDecoder.bodyData.toString(CharsetUtil.UTF_8);

        List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(body, CharsetUtil.UTF_8);
        if (nameValuePairs == null || nameValuePairs.size() == 0) {
            return null;
        }

        for(NameValuePair nameValuePair : nameValuePairs) {
            if (name.equals(nameValuePair.getName())) {
                return ReflectionUtil.castTo(type, nameValuePair.getValue());
            }
        }

        return null;
    }

}
