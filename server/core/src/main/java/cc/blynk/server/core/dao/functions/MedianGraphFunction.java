package cc.blynk.server.core.dao.functions;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public class MedianGraphFunction implements GraphFunction {

    private final ArrayList<Double> array;

    public MedianGraphFunction() {
        this.array = new ArrayList<>();
    }

    @Override
    public void apply(double newValue) {
        array.add(newValue);
    }

    @Override
    public double getResult() {
        Collections.sort(array);
        int middle = array.size() / 2;
        if (array.size() % 2 == 0) {
            return (array.get(middle) + array.get(middle - 1)) / 2;
        }
        return array.get(middle);
    }

}
