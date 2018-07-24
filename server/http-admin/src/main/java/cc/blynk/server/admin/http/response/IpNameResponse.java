package cc.blynk.server.admin.http.response;

import java.util.Objects;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.12.15.
 */
public final class IpNameResponse {

    public final int id;

    public final String name;

    public final String ip;

    public final String type;

    public IpNameResponse(int id, String name, String ip, String type) {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IpNameResponse that = (IpNameResponse) o;
        return Objects.equals(name, that.name)
                && Objects.equals(ip, that.ip)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ip, type);
    }
}
