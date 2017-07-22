package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class MaxFunction extends Function {

    private double value = Double.MIN_VALUE;

    @Override
    public void apply(double value) {
        this.value = Math.max(value, value);
    }

    @Override
    public double getResult() {
        return value;
    }
}
