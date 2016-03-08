package cc.blynk.server.core;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetGraphDataBinaryMessage;
import cc.blynk.server.core.reporting.GraphPinRequest;
import cc.blynk.utils.StateHolderUtil;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static cc.blynk.utils.ByteUtils.*;

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

    //todo move to properties
    private static final int NOTIFICATIONS_PROCESSORS = 5;

    private final ReportingDao reportingDao;
    private final ThreadPoolExecutor executor;
    private final Logger log = LogManager.getLogger(BlockingIOProcessor.class);
    public volatile String tokenBody;

    public BlockingIOProcessor(int maxQueueSize, String tokenBody, ReportingDao reportingDao) {
        this.reportingDao = reportingDao;
        this.executor = new ThreadPoolExecutor(
                NOTIFICATIONS_PROCESSORS, NOTIFICATIONS_PROCESSORS,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
        this.tokenBody = tokenBody;
    }

    public void readGraphData(Channel channel, String name, GraphPinRequest[] requestedPins, int msgId) {
        executor.execute(() -> {
            try {
                byte[][] data = reportingDao.getAllFromDisk(name, requestedPins, msgId);
                byte[] compressed = compress(requestedPins[0].dashId, data, msgId);

                log.trace("Sending getGraph response. ");
                channel.eventLoop().execute(() -> {
                    channel.writeAndFlush(new GetGraphDataBinaryMessage(msgId, compressed));
                });
            } catch (Exception e) {
                log(channel, e.getMessage(), msgId, Response.NO_DATA_EXCEPTION);
            }
        });
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private void log(Channel channel, String errorMessage, int msgId, int response) {
        User user = StateHolderUtil.getStateUser(channel);
        if (user != null) {
            log(user.name, errorMessage);

            channel.eventLoop().execute(() -> {
                channel.writeAndFlush(new ResponseMessage(msgId, response));
            });
        }
    }

    private void log(String username, String errorMessage) {
        ThreadContext.put("user", username);
        log.error("Error performing blocking IO. {}", errorMessage);
        ThreadContext.clearMap();
    }

}
