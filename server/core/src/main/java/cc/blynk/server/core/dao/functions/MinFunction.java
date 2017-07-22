package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class MinFunction extends Function {

    private double value = Double.MAX_VALUE;

    @Override
    public void apply(double value) {
        this.value = Math.min(value, value);
    }

    @Override
    public double getResult() {
        return value;
    }

}
