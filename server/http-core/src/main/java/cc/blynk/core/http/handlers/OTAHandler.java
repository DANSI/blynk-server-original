/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cc.blynk.core.http.handlers;

import cc.blynk.core.http.Response;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.TokenValue;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.core.http.Response.badRequest;
import static cc.blynk.core.http.Response.ok;
import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

public class OTAHandler extends UploadHandler {

    private static final Logger log = LogManager.getLogger(OTAHandler.class);

    private final TokenManager tokenManager;
    private final SessionDao sessionDao;
    private final String serverHostUrl;

    public OTAHandler(Holder holder, String handlerUri, String uploadFolder) {
        super(handlerUri, uploadFolder);
        this.tokenManager = holder.tokenManager;
        this.sessionDao = holder.sessionDao;
        this.serverHostUrl = "http://" + holder.props.getServerHost();
    }

    @Override
    public boolean accept(HttpRequest req) {
        return req.method() == HttpMethod.POST && req.uri().endsWith(handlerUri);
    }

    @Override
    public Response afterUpload(String path) {
        String token = uri.substring(uri.indexOf("/") + 1, uri.indexOf("/", 2));
        TokenValue tokenValue = tokenManager.getUserByToken(token);

        if (tokenValue == null) {
            log.debug("Requested token {} not found.", token);
            return badRequest("Invalid token.");
        }

        User user = tokenValue.user;
        int dashId = tokenValue.dashId;
        int deviceId = tokenValue.deviceId;

        Session session = sessionDao.userSession.get(new UserKey(user));
        if (session == null) {
            log.debug("No session for user {}.", user.email);
            return badRequest("Device wasn't connected yet.");
        }

        String otaServerUrl = serverHostUrl + path;
        String body = "ota" + BODY_SEPARATOR + otaServerUrl;
        if (session.sendMessageToHardware(dashId, BLYNK_INTERNAL, 7777, body, deviceId)) {
            log.debug("No device in session.");
            return badRequest("No device in session.");
        }

        return ok(path);
    }

}
