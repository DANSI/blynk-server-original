package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class AverageGraphFunction implements GraphFunction {

    private int count;
    private double sum;

    public AverageGraphFunction() {
        this.count = 0;
        this.sum = 0;
    }

    @Override
    public void apply(double newValue) {
        this.count++;
        this.sum += newValue;
    }

    @Override
    public double getResult() {
        return sum / count;
    }

}
