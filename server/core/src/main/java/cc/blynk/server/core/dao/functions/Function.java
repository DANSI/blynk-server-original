package cc.blynk.server.core.dao.functions;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.07.17.
 */
public abstract class Function {

    public abstract void apply(double value);

    public abstract double getResult();

}
