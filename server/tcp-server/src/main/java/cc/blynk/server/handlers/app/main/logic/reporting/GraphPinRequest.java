package cc.blynk.server.handlers.app.main.logic.reporting;

import cc.blynk.server.model.enums.GraphType;
import cc.blynk.server.model.enums.PinType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.10.15.
 */
public abstract class GraphPinRequest {

    public int dashId;

    public PinType pinType;

    public byte pin;

    public int count;

    public GraphType type;

}
