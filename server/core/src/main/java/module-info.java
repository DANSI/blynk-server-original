/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.05.18.
 */
module cc.blynk.core {
    exports cc.blynk.server.core.dao;
    exports cc.blynk.server;
    exports cc.blynk.server.core.dao.ota;
    exports cc.blynk.server.core.model.auth;
    exports cc.blynk.server.core.protocol.enums;
    exports cc.blynk.server.core.protocol.handlers;
    exports cc.blynk.server.core.model.serialization;
    exports cc.blynk.server.core.stats;
    exports cc.blynk.server.core.model;
    exports cc.blynk.server.core.stats.model;
    exports cc.blynk.server.core;
    exports cc.blynk.server.core.model.enums;
    exports cc.blynk.server.core.model.storage;
    exports cc.blynk.server.core.model.widgets;
    exports cc.blynk.server.core.model.widgets.notifications;
    exports cc.blynk.server.core.model.widgets.others.rtc;
    exports cc.blynk.server.core.model.widgets.ui.tiles;
    exports cc.blynk.server.core.processors;
    exports cc.blynk.server.core.protocol.exceptions;
    exports cc.blynk.server.db;
    exports cc.blynk.server.internal;
    exports cc.blynk.server.handlers;
    exports cc.blynk.server.core.model.device;
    requires cc.blynk.server.notifications.mail;
    requires cc.blynk.server.notifications.push;
    requires cc.blynk.server.notifications.sms;
    requires cc.blynk.server.notifications.twitter;
    requires cc.blynk.utils;
    requires io.netty.transport.epoll;
    requires io.netty.common;
    requires io.netty.transport;
    requires io.netty.handler;
    requires io.netty.buffer;
    requires io.netty.codec;
    requires io.netty.codec.http;
    requires async.http.client;
    requires cc.blynk.server.acme;
    requires org.apache.logging.log4j;
    requires com.zaxxer.hikari;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
}