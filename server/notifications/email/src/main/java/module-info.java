/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.09.17.
 */
module email {
    requires log4j.api;
    requires javaee.api;
    requires java.activation;

    exports cc.blynk.server.notifications.mail;
}