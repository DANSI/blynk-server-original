package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class MinGraphFunction implements GraphFunction {

    private double value = Double.MAX_VALUE;

    @Override
    public void apply(double newValue) {
        this.value = Math.min(value, newValue);
    }

    @Override
    public double getResult() {
        return value;
    }

}
