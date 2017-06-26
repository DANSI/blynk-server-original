package cc.blynk.server.core.model.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.09.16.
 */
public class FacebookTokenResponse {

    public final String email;

    @JsonCreator
    public FacebookTokenResponse(@JsonProperty("email") String email) {
        this.email = email;
    }

}
