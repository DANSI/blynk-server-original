package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class AverageFunction implements Function {

    private int count;
    private double sum;

    public AverageFunction() {
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
