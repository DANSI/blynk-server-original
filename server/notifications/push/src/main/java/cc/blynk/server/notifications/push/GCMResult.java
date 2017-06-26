package cc.blynk.server.notifications.push;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMResult {

    public final String error;

    @JsonCreator
    public GCMResult(@JsonProperty("error") String error) {
        this.error = error;
    }
}
