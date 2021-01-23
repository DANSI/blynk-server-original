package cc.blynk.core.http.handlers;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Full copy of HttpPostRequestDecoder to fix
 * https://github.com/netty/netty/issues/10281
 */
public class BlynkHttpPostRequestDecoder implements InterfaceHttpPostRequestDecoder {

    private final InterfaceHttpPostRequestDecoder decoder;

    /**
     *
     * @param request
     *            the request to decode
     * @throws NullPointerException
     *             for request
     * @throws HttpPostRequestDecoder.ErrorDataDecoderException
     *             if the default charset was wrong when decoding or other
     *             errors
     */
    public BlynkHttpPostRequestDecoder(HttpRequest request) {
        this(new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE), request, HttpConstants.DEFAULT_CHARSET);
    }

    /**
     *
     * @param factory
     *            the factory used to create InterfaceHttpData
     * @param request
     *            the request to decode
     * @throws NullPointerException
     *             for request or factory
     * @throws HttpPostRequestDecoder.ErrorDataDecoderException
     *             if the default charset was wrong when decoding or other
     *             errors
     */
    public BlynkHttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request) {
        this(factory, request, HttpConstants.DEFAULT_CHARSET);
    }

    /**
     *
     * @param factory
     *            the factory used to create InterfaceHttpData
     * @param request
     *            the request to decode
     * @param charset
     *            the charset to use as default
     * @throws NullPointerException
     *             for request or charset or factory
     * @throws HttpPostRequestDecoder.ErrorDataDecoderException
     *             if the default charset was wrong when decoding or other
     *             errors
     */
    public BlynkHttpPostRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset) {
        ObjectUtil.checkNotNull(factory, "factory");
        ObjectUtil.checkNotNull(request, "request");
        ObjectUtil.checkNotNull(charset, "charset");

        // Fill default values
        if (isMultipart(request)) {
            decoder = new BlynkHttpPostMultipartRequestDecoder(factory, request, charset);
        } else {
            decoder = new HttpPostStandardRequestDecoder(factory, request, charset);
        }
    }

    /**
     * Check if the given request is a multipart request
     * @return True if the request is a Multipart request
     */
    public static boolean isMultipart(HttpRequest request) {
        String mimeType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (mimeType != null && mimeType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) {
            return getMultipartDataBoundary(mimeType) != null;
        }
        return false;
    }

    /**
     * Check from the request ContentType if this request is a Multipart request.
     * @return an array of String if multipartDataBoundary exists with the multipartDataBoundary
     * as first element, charset if any as second (missing if not set), else null
     */
    protected static String[] getMultipartDataBoundary(String contentType) {
        // Check if Post using "multipart/form-data; boundary=--89421926422648 [; charset=xxx]"
        String[] headerContentType = splitHeaderContentType(contentType);
        final String multiPartHeader = HttpHeaderValues.MULTIPART_FORM_DATA.toString();
        if (headerContentType[0].regionMatches(true, 0, multiPartHeader, 0, multiPartHeader.length())) {
            int mrank;
            int crank;
            final String boundaryHeader = HttpHeaderValues.BOUNDARY.toString();
            if (headerContentType[1].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
                mrank = 1;
                crank = 2;
            } else if (headerContentType[2].regionMatches(true, 0, boundaryHeader, 0, boundaryHeader.length())) {
                mrank = 2;
                crank = 1;
            } else {
                return null;
            }
            String boundary = StringUtil.substringAfter(headerContentType[mrank], '=');
            if (boundary == null) {
                throw new HttpPostRequestDecoder.ErrorDataDecoderException("Needs a boundary value");
            }
            if (boundary.charAt(0) == '"') {
                String bound = boundary.trim();
                int index = bound.length() - 1;
                if (bound.charAt(index) == '"') {
                    boundary = bound.substring(1, index);
                }
            }
            final String charsetHeader = HttpHeaderValues.CHARSET.toString();
            if (headerContentType[crank].regionMatches(true, 0, charsetHeader, 0, charsetHeader.length())) {
                String charset = StringUtil.substringAfter(headerContentType[crank], '=');
                if (charset != null) {
                    return new String[] {"--" + boundary, charset};
                }
            }
            return new String[] {"--" + boundary};
        }
        return null;
    }

    @Override
    public boolean isMultipart() {
        return decoder.isMultipart();
    }

    @Override
    public void setDiscardThreshold(int discardThreshold) {
        decoder.setDiscardThreshold(discardThreshold);
    }

    @Override
    public int getDiscardThreshold() {
        return decoder.getDiscardThreshold();
    }

    @Override
    public List<InterfaceHttpData> getBodyHttpDatas() {
        return decoder.getBodyHttpDatas();
    }

    @Override
    public List<InterfaceHttpData> getBodyHttpDatas(String name) {
        return decoder.getBodyHttpDatas(name);
    }

    @Override
    public InterfaceHttpData getBodyHttpData(String name) {
        return decoder.getBodyHttpData(name);
    }

    @Override
    public InterfaceHttpPostRequestDecoder offer(HttpContent content) {
        return decoder.offer(content);
    }

    @Override
    public boolean hasNext() {
        return decoder.hasNext();
    }

    @Override
    public InterfaceHttpData next() {
        return decoder.next();
    }

    @Override
    public InterfaceHttpData currentPartialHttpData() {
        return decoder.currentPartialHttpData();
    }

    @Override
    public void destroy() {
        decoder.destroy();
    }

    @Override
    public void cleanFiles() {
        decoder.cleanFiles();
    }

    @Override
    public void removeHttpDataFromClean(InterfaceHttpData data) {
        decoder.removeHttpDataFromClean(data);
    }

    /**
     * Split the very first line (Content-Type value) in 3 Strings
     *
     * @return the array of 3 Strings
     */
    private static String[] splitHeaderContentType(String sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;
        aStart = HttpPostBodyUtil.findNonWhitespace(sb, 0);
        aEnd =  sb.indexOf(';');
        if (aEnd == -1) {
            return new String[] {sb, "", ""};
        }
        bStart = HttpPostBodyUtil.findNonWhitespace(sb, aEnd + 1);
        if (sb.charAt(aEnd - 1) == ' ') {
            aEnd--;
        }
        bEnd =  sb.indexOf(';', bStart);
        if (bEnd == -1) {
            bEnd = HttpPostBodyUtil.findEndOfString(sb);
            return new String[] {sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), ""};
        }
        cStart = HttpPostBodyUtil.findNonWhitespace(sb, bEnd + 1);
        if (sb.charAt(bEnd - 1) == ' ') {
            bEnd--;
        }
        cEnd = HttpPostBodyUtil.findEndOfString(sb);
        return new String[] {sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), sb.substring(cStart, cEnd)};
    }

}
