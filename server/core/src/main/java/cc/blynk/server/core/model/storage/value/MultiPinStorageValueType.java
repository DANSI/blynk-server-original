package cc.blynk.server.core.model.storage.value;

import cc.blynk.utils.structure.BaseLimitedQueue;
import cc.blynk.utils.structure.LCDLimitedQueue;
import cc.blynk.utils.structure.TableLimitedQueue;
import cc.blynk.utils.structure.TerminalLimitedQueue;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27/04/2018.
 *
 */
public enum MultiPinStorageValueType {

    //WARNING: order change is not allowed!
    LCD,
    TERMINAL,
    TABLE;

    public BaseLimitedQueue<String> getQueue() {
        switch (this) {
            case LCD:
                return new LCDLimitedQueue<>();
            case TERMINAL:
                return new TerminalLimitedQueue<>();
            case TABLE:
                return new TableLimitedQueue<>();
            default:
                throw new RuntimeException("not supported");
        }
    }

}
