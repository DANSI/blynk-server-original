package cc.blynk.server.core.model.serialization;

import cc.blynk.server.core.model.storage.PinStorageKey;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * User who see shared dashboard should not see authentification data of original user
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.12.16.
 */
public abstract class DashboardMixIn {

    @JsonIgnore
    public String sharedToken;

    @JsonIgnore
    public Map<PinStorageKey, String> pinsStorage;

}
