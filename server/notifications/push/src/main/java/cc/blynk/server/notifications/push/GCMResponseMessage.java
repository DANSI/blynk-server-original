package cc.blynk.server.notifications.push;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMResponseMessage {

    private final int success;

    final int failure;

    private final long multicast_id;

    public final GCMResult[] results;

    @JsonCreator
    public GCMResponseMessage(@JsonProperty("success") int success,
                              @JsonProperty("failure") int failure,
                              @JsonProperty("multicast_id") long multicast_id,
                              @JsonProperty("results") GCMResult[] results) {
        this.success = success;
        this.failure = failure;
        this.multicast_id = multicast_id;
        this.results = results;
    }
}
