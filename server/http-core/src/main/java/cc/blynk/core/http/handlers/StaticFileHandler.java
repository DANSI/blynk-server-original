package cc.blynk.core.http.handlers;

import cc.blynk.utils.ContentTypeUtil;
import cc.blynk.utils.ServerProperties;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.12.15.
 */
public class StaticFileHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(StaticFileHandler.class);

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    /**
     * Used for case when server started from IDE and static files wasn't unpacked from jar.
     */
    private final boolean isUnpacked;
    private final StaticFile[] staticPaths;

    public StaticFileHandler(boolean isUnpacked, StaticFile... staticPaths) {
        this.staticPaths = staticPaths;
        this.isUnpacked = isUnpacked;
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx
     *            Context
     */
    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response
     *            HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private static void setDateAndCacheHeaders(io.netty.handler.codec.http.HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            return;
        }

        FullHttpRequest req = (FullHttpRequest) msg;

        StaticFile staticFile = getStaticPath(req.getUri());
        if (staticFile != null) {
            try {
                serveStatic(ctx, req, staticFile);
            } finally {
                ReferenceCountUtil.release(req);
            }
            return;
        }

        ctx.fireChannelRead(req);
    }

    private StaticFile getStaticPath(String path) {
        for (StaticFile staticPath : staticPaths) {
            if (path.startsWith(staticPath.path)) {
                return staticPath;
            }
        }
        return null;
    }

    private void serveStatic(ChannelHandlerContext ctx, FullHttpRequest request, StaticFile staticFile) throws Exception {
        if (!request.getDecoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (request.getMethod() != HttpMethod.GET) {
            return;
        }

        File file;
        //running from jar
        if (isUnpacked) {
            //.substring(1) is all after "/" part
            if (staticFile.isDirectPath) {
                file = new File(request.getUri());
            } else {
                file = ServerProperties.getFileInCurrentDir(request.getUri()).toFile();
            }
        } else {
            //for local mode / running from ide
            file = new File("./server/http-admin/target/classes" + request.getUri());
            if (!file.exists()) {
                file = new File("./server/http-api/target/classes" + request.getUri());
            }
        }

        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        // Cache Validation
        String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return;
            }
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        io.netty.handler.codec.http.HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        HttpHeaders.setContentLength(response, fileLength);

        //setting content type
        response.headers().set(CONTENT_TYPE, ContentTypeUtil.getContentType(file.getName()));

        setDateAndCacheHeaders(response, file);
        if (HttpHeaders.isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            sendFileFuture =
                    ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                            ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }

        // Decide whether to close the connection or not.
        if (!HttpHeaders.isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause.getMessage() != null && cause.getMessage().contains("unknown_ca")) {
            log.warn("Self-generated certificate.");
        } else {
            cause.printStackTrace();
            if (ctx.channel().isActive()) {
                sendError(ctx, INTERNAL_SERVER_ERROR);
            }
        }
    }

}
