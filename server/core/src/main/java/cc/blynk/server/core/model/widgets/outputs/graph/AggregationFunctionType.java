package cc.blynk.server.core.model.widgets.outputs.graph;

import cc.blynk.server.core.dao.functions.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public enum AggregationFunctionType {

    MIN(MinFunction.class),
    MAX(MaxFunction.class),
    AVG(AverageFunction.class),
    SUM(SumFunction.class),
    MED(MedianFunction.class);

    private Class<? extends Function> clazz;

    AggregationFunctionType(Class<? extends Function> clazz) {
        this.clazz = clazz;
    }

    public Function produce() {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

}
