/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.09.17.
 */
module cc.blynk.server.notifications.sms {
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires async.http.client;
    requires com.fasterxml.jackson.annotation;

    exports cc.blynk.server.notifications.sms;
}