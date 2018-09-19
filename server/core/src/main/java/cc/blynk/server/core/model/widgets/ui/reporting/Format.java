package cc.blynk.server.core.model.widgets.ui.reporting;

public enum Format {

    ISO_SIMPLE("yyyy-MM-dd HH:mm:ss"), //2018-06-07 22:01:20
    ISO_INSTANT("yyyy-MM-dd'T'HH:mm:ssXXX"), //2018-06-07T22:01:20+03:00
    ISO_INSTANT_Z("yyyy-MM-dd'T'HH:mm:ssz"), //2018-06-07T22:01:20EEST
    TS(null);

    public final String pattern;

    Format(String pattern) {
        this.pattern = pattern;
    }
}
