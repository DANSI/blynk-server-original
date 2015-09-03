package cc.blynk.server.storage.reporting.average;

import java.io.Serializable;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class AggregationValue implements Serializable {

    private final DoubleAdder values = new DoubleAdder();
    private final LongAdder count = new LongAdder();

    public void update(double val) {
        values.add(val);
        count.increment();
    }

    public double calcAverage() {
        return values.sum() / count.sum();
    }
}
