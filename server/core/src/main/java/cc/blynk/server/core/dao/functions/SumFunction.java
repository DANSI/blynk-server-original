package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class SumFunction extends Function {

    private double sum;

    public SumFunction() {
        this.sum = 0;
    }

    @Override
    public void apply(double value) {
        this.sum += value;
    }

    @Override
    public double getResult() {
        return sum;
    }

}
