/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.10.17.
 */
module cc.blynk.server.acme {
    requires acme4j.client;
    requires org.apache.logging.log4j;
    requires acme4j.utils;

    exports cc.blynk.server.acme;
}