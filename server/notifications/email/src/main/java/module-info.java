/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.05.18.
 */
module cc.blynk.server.notifications.mail {
    requires org.apache.logging.log4j;
    requires cc.blynk.utils;
    requires java.mail;
    requires java.activation;

    exports cc.blynk.server.notifications.mail;
}