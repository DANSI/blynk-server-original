package cc.blynk.server.dao.graph;

import cc.blynk.common.utils.StringUtils;
import cc.blynk.server.exceptions.IllegalCommandException;
import cc.blynk.server.model.Profile;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 *
 * todo redesign. right now it is not efficient at all
 */
public class GraphInMemoryStorage implements Storage {

    private final BlockingQueue<QueueMessage> storeQueue = new LinkedBlockingDeque<>();
    private final Map<GraphKey, Queue<String>> userValues;

    public GraphInMemoryStorage(int sizeLimit) {
        this.userValues = new ConcurrentHashMap<>();
        new StoreProcessor(sizeLimit).start();
    }

    private static String attachTS(String body, long ts) {
        if (body.charAt(body.length() - 1) == StringUtils.BODY_SEPARATOR) {
            return body + ts;
        } else {
            return body + StringUtils.BODY_SEPARATOR + ts;
        }
    }

    @Override
    public String store(Profile profile, Integer dashId, String body, int msgId) {
        if (body.length() < 4) {
            throw new IllegalCommandException("Hardware command body too short.", msgId);
        }

        if (body.charAt(1) == 'w') {
            Byte pin;
            try {
                pin = Byte.valueOf(StringUtils.fetchPin(body));
            } catch (NumberFormatException e) {
                throw new IllegalCommandException("Hardware command body incorrect.", msgId);
            }

            GraphKey key = new GraphKey(dashId, pin);
            if (profile.hasGraphPin(key)) {
                long ts = System.currentTimeMillis();
                body = attachTS(body, ts);
                storeQueue.offer(new QueueMessage(key, body));
            }
        }
        return body;
    }

    @Override
    public Queue<String> getAll(GraphKey key) {
        return userValues.get(key);
    }

    private class StoreProcessor extends Thread {

        private final int sizeLimit;

        StoreProcessor(int sizeLimit) {
            this.sizeLimit = sizeLimit;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    QueueMessage message = storeQueue.take();
                    Queue<String> values = userValues.get(message.key);
                    if (values == null) {
                        values = new LinkedList<>();
                        userValues.put(message.key, values);
                    }

                    if (values.size() == sizeLimit) {
                        values.remove();
                    }
                    values.add(message.body);
                } catch (InterruptedException e) {
                }
            }
        }

    }

}
