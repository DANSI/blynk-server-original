package cc.blynk.server.model.widgets.others;

import cc.blynk.server.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class Mail extends Widget {

    public String to;

    public String subj;

    public String body;

    public Mail() {
    }

    public Mail(String to, String subj, String body) {
        this.to = to;
        this.subj = subj;
        this.body = body;
    }
}
