package cc.blynk.server.core.model.widgets.others.eventor.model.condition;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class NotEqual extends BaseCondition {

    public double value;

    @Override
    public boolean isValid(double in) {
        return in != value;
    }

}
