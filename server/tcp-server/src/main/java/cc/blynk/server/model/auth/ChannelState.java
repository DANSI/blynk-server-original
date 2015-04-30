package cc.blynk.server.model.auth;

import io.netty.util.AttributeKey;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.03.15.
 */
public class ChannelState {

    public static final AttributeKey<User> USER = AttributeKey.valueOf("user");
    public static final AttributeKey<Integer> DASH_ID = AttributeKey.valueOf("dashId");
    public static final AttributeKey<Boolean> IS_HARD_CHANNEL = AttributeKey.valueOf("isHardwareChannel");
    public static final AttributeKey<String> TOKEN = AttributeKey.valueOf("token");
    public static final AttributeKey<Map<String, String>> SEND_TO_TOKEN = AttributeKey.valueOf("sendToToken");

}
