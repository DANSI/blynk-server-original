/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.09.17.
 */
module sms {
    requires com.fasterxml.jackson.databind;
    requires netty.codec.http;
    requires log4j.api;
    requires async.http.client;
    requires jackson.annotations;

    exports cc.blynk.server.notifications.sms;
}