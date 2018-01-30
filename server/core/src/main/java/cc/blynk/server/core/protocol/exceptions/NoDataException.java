package cc.blynk.server.core.protocol.exceptions;

/**
 * This exception doesn't inherit BaseServerException
 * as it is only used as marker.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class NoDataException extends Exception {

    public NoDataException() {
        super("No data.");
    }
}
