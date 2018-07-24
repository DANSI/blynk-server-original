package cc.blynk.server.admin.http.response;

import java.util.Objects;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.12.15.
 */
public final class IpNameResponse {

    public final String name;

    public final String ip;

    public IpNameResponse(String name, String ip) {
        this.name = name;
        this.ip = ip;
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
        return Objects.equals(ip, that.ip)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, name);
    }
}
