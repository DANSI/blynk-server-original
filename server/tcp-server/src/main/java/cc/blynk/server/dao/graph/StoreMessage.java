package cc.blynk.server.dao.graph;

import cc.blynk.common.utils.StringUtils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.07.15.
 */
public class StoreMessage {

    public GraphKey key;

    public String body;

    public long ts;

    public StoreMessage(GraphKey key, String body, long ts) {
        this.key = key;
        this.body = body;
        this.ts = ts;
    }

    @Override
    public String toString() {
        if (body.charAt(body.length() - 1) == StringUtils.BODY_SEPARATOR) {
            return body + ts;
        } else {
            return body + StringUtils.BODY_SEPARATOR + ts;
        }
    }

    public String toCSV() {
        return key.dashId + "," + key.pin + "," + body.substring(body.lastIndexOf(StringUtils.BODY_SEPARATOR) + 1) + "," + ts;
    }
}
