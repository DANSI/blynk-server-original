package cc.blynk.server.core.model.storage.value;

import cc.blynk.server.core.model.storage.key.DashPinStorageKey;
import io.netty.channel.Channel;

import java.util.Collection;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27/04/2018.
 *
 */
public abstract class PinStorageValue {

    public abstract void update(String value);

    public abstract Collection<String> values();

    public abstract void sendAppSync(Channel appChannel, int dashId, DashPinStorageKey key);

}
