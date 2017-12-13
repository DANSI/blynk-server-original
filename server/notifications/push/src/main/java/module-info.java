/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.09.17.
 */
module cc.blynk.server.notifications.push {
    requires log4j.api;
    requires jackson.annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.netty.codec.http;
    requires async.http.client;

    exports cc.blynk.server.notifications.push;
    exports cc.blynk.server.notifications.push.android;
    exports cc.blynk.server.notifications.push.enums;
    exports cc.blynk.server.notifications.push.ios;
}
