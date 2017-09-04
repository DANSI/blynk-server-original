package cc.blynk.server.core.model.widgets.outputs.graph;

import cc.blynk.server.core.dao.functions.AverageFunction;
import cc.blynk.server.core.dao.functions.Function;
import cc.blynk.server.core.dao.functions.MaxFunction;
import cc.blynk.server.core.dao.functions.MedianFunction;
import cc.blynk.server.core.dao.functions.MinFunction;
import cc.blynk.server.core.dao.functions.SumFunction;

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

    public Function produce() {
        switch (this) {
            case MIN :
                return new MinFunction();
            case MAX :
                return new MaxFunction();
            case SUM :
                return new SumFunction();
            case MED :
                return new MedianFunction();
            default:
                return new AverageFunction();
        }
    }

}
