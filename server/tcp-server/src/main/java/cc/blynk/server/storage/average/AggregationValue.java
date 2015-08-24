package cc.blynk.server.storage.average;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class AggregationValue {

    double sum;
    int count;

    public AggregationValue(double val) {
        this.sum = val;
        this.count = 1;
    }

    public void update(double val) {
        this.sum += val;
        count++;
    }

    public double calcAverage() {
        return sum / count;
    }
}
