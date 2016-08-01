package cc.blynk.server.core.model.widgets.others.eventor.model.condition;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Between extends BaseCondition {

    public double left;

    public double right;

    @Override
    public boolean isValid(double in) {
        return (left < in) && (in < right);
    }

}
