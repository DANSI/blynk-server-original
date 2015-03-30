package cc.blynk.server.model.auth;

import io.netty.util.AttributeKey;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.03.15.
 */
public class ChannelState {

    public static final AttributeKey<User> USER = AttributeKey.valueOf("user");
    public static final AttributeKey<Integer> DASH_ID = AttributeKey.valueOf("dashId");
    public static final AttributeKey<Boolean> IS_HARD_CHANNEL = AttributeKey.valueOf("isHardwareChannel");

}
