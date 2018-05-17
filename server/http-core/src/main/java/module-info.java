/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.05.18.
 */
module cc.blynk.http.core {
    exports cc.blynk.core.http;
    exports cc.blynk.core.http.annotation;
    exports cc.blynk.core.http.utils;
    exports cc.blynk.core.http.model;
    requires io.netty.transport;
    requires io.netty.codec.http;
    requires cc.blynk.core;
    requires io.netty.common;
    requires org.apache.logging.log4j;
    requires cc.blynk.utils;
    requires io.netty.buffer;
    requires io.netty.handler;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
}