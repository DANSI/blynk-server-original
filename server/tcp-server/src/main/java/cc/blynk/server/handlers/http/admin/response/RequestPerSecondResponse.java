package cc.blynk.server.handlers.http.admin.response;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.12.15.
 */
public class RequestPerSecondResponse {

    public String name;

    public int appRate;

    public int hardRate;

    public RequestPerSecondResponse(String name, int appRate, int hardRate) {
        this.name = name;
        this.appRate = appRate;
        this.hardRate = hardRate;
    }


}
