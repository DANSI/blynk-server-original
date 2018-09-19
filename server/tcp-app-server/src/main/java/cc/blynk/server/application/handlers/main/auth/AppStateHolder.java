package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.session.StateHolderBase;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class AppStateHolder extends StateHolderBase {

    public final Version version;

    public AppStateHolder(User user, Version version) {
        super(user);
        this.version = version;
    }

    @Override
    public boolean contains(String sharedToken) {
        return true;
    }

    @Override
    public boolean isSameDash(int inDashId) {
        return true;
    }

    @Override
    public boolean isSameDevice(int deviceId) {
        return true;
    }

    @Override
    public boolean isSameDashAndDeviceId(int inDashId, int deviceId) {
        return true;
    }

    public boolean isNewSyncFormat() {
        return version.versionSingleNumber >= 22600;
    }
}
