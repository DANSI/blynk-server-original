package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class MaxGraphFunction implements GraphFunction {

    private double value = Double.MIN_VALUE;

    @Override
    public void apply(double newValue) {
        this.value = Math.max(value, newValue);
    }

    @Override
    public double getResult() {
        return value;
    }
}
