package cc.blynk.server.core.model.serialization;

public class View {

    /**
     * special marker class that used to serialize all fields expect those one
     * that marked with Private annotation
     */
    public static class PublicOnly {
    }

    /**
     * special utility class that is used to mark private fields
     * that should not always be visible to the end users.
     */
    public static class Private {
    }

    public static class HttpAPIField {
    }

}
