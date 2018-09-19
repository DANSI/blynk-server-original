package cc.blynk.server.hardware.internal;

import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.protocol.model.messages.StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.18.
 */
public class BridgeForwardMessage {

    public final StringMessage message;

    public final TokenValue tokenValue;

    public final UserKey userKey;

    public BridgeForwardMessage(StringMessage bridgeMessage, TokenValue tokenValue, UserKey userKey) {
        this.message = bridgeMessage;
        this.tokenValue = tokenValue;
        this.userKey = userKey;
    }
}

