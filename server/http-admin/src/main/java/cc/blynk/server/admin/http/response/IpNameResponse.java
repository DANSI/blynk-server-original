package cc.blynk.server.admin.http.response;

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

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) {
            return false;
        }
        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
