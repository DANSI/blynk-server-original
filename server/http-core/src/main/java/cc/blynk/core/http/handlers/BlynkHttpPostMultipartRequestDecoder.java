package cc.blynk.core.http.handlers;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;

import java.nio.charset.Charset;

/**
 * Full copy of HttpPostRequestDecoder to fix
 * https://github.com/netty/netty/issues/10281
 */
public class BlynkHttpPostMultipartRequestDecoder extends HttpPostMultipartRequestDecoder {

    private final boolean state;

    public BlynkHttpPostMultipartRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset) {
        super(factory, request, charset);
        this.state = true;
    }

    @Override
    public HttpPostMultipartRequestDecoder offer(HttpContent content) {
        //this is very dirty hack
        //we skip the first invocation of the offer
        //it wont work for every case, but we have Aggregate handler in the pipeline
        //so we are covered here :)
        if (state) {
            return super.offer(content);
        }
        return null;
    }
}
