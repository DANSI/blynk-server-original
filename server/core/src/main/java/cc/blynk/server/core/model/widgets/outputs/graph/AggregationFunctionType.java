package cc.blynk.server.core.model.widgets.outputs.graph;

import cc.blynk.server.core.dao.functions.AverageGraphFunction;
import cc.blynk.server.core.dao.functions.GraphFunction;
import cc.blynk.server.core.dao.functions.MaxGraphFunction;
import cc.blynk.server.core.dao.functions.MedianGraphFunction;
import cc.blynk.server.core.dao.functions.MinGraphFunction;
import cc.blynk.server.core.dao.functions.SumGraphFunction;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public enum AggregationFunctionType {

    MIN,
    MAX,
    AVG,
    SUM,
    MED;

    public GraphFunction produce() {
        switch (this) {
            case MIN :
                return new MinGraphFunction();
            case MAX :
                return new MaxGraphFunction();
            case SUM :
                return new SumGraphFunction();
            case MED :
                return new MedianGraphFunction();
            default:
                return new AverageGraphFunction();
        }
    }

}
