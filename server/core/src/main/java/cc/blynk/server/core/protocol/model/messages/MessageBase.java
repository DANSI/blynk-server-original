package cc.blynk.server.core.protocol.model.messages;

import cc.blynk.server.core.protocol.enums.Command;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 * Yes, I don't use getters and setters, inlining is not always works as expected.
 *
 * IMPORTANT : have in mind, in body we retrieve always unsigned bytes, shorts, while in java
 * is only signed types, so we require 2 times larger types.
 */
public abstract class MessageBase {

    public final short command;

    public final int id;

    public MessageBase(int id, short command) {
        this.command = command;
        this.id = id;
    }

    public abstract byte[] getBytes();

    @Override
    public String toString() {
        return "id=" + id
                + ", command=" + Command.getNameByValue(command);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MessageBase that = (MessageBase) o;

        if (command != that.command) {
            return false;
        }
        return id == that.id;
    }

    @Override
    public int hashCode() {
        int result = (int) command;
        result = 31 * result + id;
        return result;
    }
}
