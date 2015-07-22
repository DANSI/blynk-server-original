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
public class GraphInMemoryStorage {

    private final BlockingQueue<StoreMessage> storeQueue = new LinkedBlockingDeque<>();
    private final Map<GraphKey, Queue<StoreMessage>> userValues;

    public GraphInMemoryStorage(int sizeLimit) {
        this.userValues = new ConcurrentHashMap<>();
        new StoreProcessor(sizeLimit).start();
    }

    public StoreMessage store(Profile profile, Integer dashId, String body, int msgId) {
        byte pin;
        try {
            pin = Byte.parseByte(StringUtils.fetchPin(body));
        } catch (NumberFormatException e) {
            throw new IllegalCommandException("Hardware command body incorrect.", msgId);
        }

        GraphKey key = new GraphKey(dashId, pin);
        if (profile.hasGraphPin(key)) {
            StoreMessage storeMessage = new StoreMessage(key, body, System.currentTimeMillis());
            storeQueue.offer(storeMessage);
            return storeMessage;
        }

        return null;
    }

    public Queue<StoreMessage> getAll(GraphKey key) {
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
                    StoreMessage message = storeQueue.take();
                    Queue<StoreMessage> values = userValues.get(message.key);
                    if (values == null) {
                        values = new LinkedList<>();
                        userValues.put(message.key, values);
                    }

                    if (values.size() == sizeLimit) {
                        values.remove();
                    }
                    values.add(message);
                } catch (InterruptedException e) {
                }
            }
        }

    }

}
