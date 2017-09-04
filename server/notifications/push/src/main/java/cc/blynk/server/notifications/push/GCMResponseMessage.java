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

    @JsonProperty("multicast_id")
    private final long multicastId;

    public final GCMResult[] results;

    @JsonCreator
    public GCMResponseMessage(@JsonProperty("success") int success,
                              @JsonProperty("failure") int failure,
                              @JsonProperty("multicast_id") long multicastId,
                              @JsonProperty("results") GCMResult[] results) {
        this.success = success;
        this.failure = failure;
        this.multicastId = multicastId;
        this.results = results;
    }
}
