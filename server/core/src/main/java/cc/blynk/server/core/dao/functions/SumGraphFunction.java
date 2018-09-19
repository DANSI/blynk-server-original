package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class SumGraphFunction implements GraphFunction {

    private double sum;

    public SumGraphFunction() {
        this.sum = 0;
    }

    @Override
    public void apply(double newValue) {
        this.sum += newValue;
    }

    @Override
    public double getResult() {
        return sum;
    }

}
