package cc.blynk.server.core;

import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.notifications.push.GCMMessage;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.push.android.AndroidGCMMessage;
import cc.blynk.server.notifications.push.ios.IOSGCMMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around ThreadPoolExecutor that should perform blocking IO operations.
 * Due to async nature of netty performing Blocking operations withing netty pipeline
 * will cause performance issues. So Blocking operations should always
 * executed via this wrapper.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public class BlockingIOProcessor implements Closeable {

    public static final String TOKEN_MAIL_BODY = "token_mail_body.txt";
    private static final Logger log = LogManager.getLogger(BlockingIOProcessor.class);
    private final ThreadPoolExecutor executor;
    public volatile String tokenBody;

    public BlockingIOProcessor(int poolSize, int maxQueueSize, String tokenBody) {
        this.executor = new ThreadPoolExecutor(
                poolSize, poolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
        this.tokenBody = tokenBody;
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public void push(GCMWrapper gcmWrapper, String username, Notification widget, String body, int dashId) {
        if (widget.androidTokens.size() != 0) {
            for (Map.Entry<String, String> entry : widget.androidTokens.entrySet()) {
                push(gcmWrapper,
                        new AndroidGCMMessage(entry.getValue(), widget.priority, body, dashId),
                        widget.androidTokens,
                        entry.getKey(),
                        username
                );
            }
        }

        if (widget.iOSTokens.size() != 0) {
            for (Map.Entry<String, String> entry : widget.iOSTokens.entrySet()) {
                push(gcmWrapper,
                        new IOSGCMMessage(entry.getValue(), widget.priority, body, dashId),
                        widget.iOSTokens,
                        entry.getKey(),
                        username
                );
            }
        }
    }

    private void push(GCMWrapper gcmWrapper, GCMMessage message, Map<String, String> tokens, String uid, String username) {
        execute(() -> {
            try {
                gcmWrapper.send(message);
            } catch (Exception e) {
                log.error("Error sending push notification on offline hardware. For user {}. {}", username, e.getMessage());

                if (e.getMessage() != null && e.getMessage().contains("NotRegistered")) {
                    log.error("Removing invalid token. UID {}", uid);
                    tokens.remove(uid);
                }
            }
        });
    }

}
