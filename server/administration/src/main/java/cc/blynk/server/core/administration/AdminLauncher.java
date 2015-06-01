package cc.blynk.server.core.administration;

import cc.blynk.common.utils.Config;
import cc.blynk.server.core.administration.actions.ActiveUsers;
import cc.blynk.server.core.administration.actions.ActivityMonitor;
import cc.blynk.server.core.administration.actions.ManualResetPassword;
import cc.blynk.server.core.administration.actions.ResetPassword;
import cc.blynk.server.utils.ByteClassLoaderUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Utility class used for administration purposes.
 * Opens socket on 8777 port, read bytecode class into array depending on operation required,
 * read class is sent ot server where is build from bytecode and executed via Executable interface.
 *
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.04.15.
 */
public class AdminLauncher {

    private final String host;
    private final int port;

    public AdminLauncher(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        new AdminLauncher("localhost", 8777).connect(args);
    }

    private static void sendParams(String[] args, DataOutputStream outToServer) throws IOException {
        int paramsNumber = args.length - 1;
        outToServer.writeByte(paramsNumber);

        if (args.length > 1) {
            for (int i = 0; i < paramsNumber; i++) {
                String param = args[i + 1];
                outToServer.writeShort(param.length());
                outToServer.write(param.getBytes(Config.DEFAULT_CHARSET));
            }
        }
    }

    /**
     * Loads class from jar as byte array.
     *
     * @param action type of class to load.
     * @return class byte array
     */
    private static byte[] loadClass(String action) throws IOException {
        switch (action.toLowerCase()) {
            case "resetpassword" :
                return readClassAsResource(ResetPassword.class);
            case "manualresetpassword" :
                return readClassAsResource(ManualResetPassword.class);
            case "quotausage" :
                return readClassAsResource(ActivityMonitor.class);
            case "activeusers" :
                return readClassAsResource(ActiveUsers.class);
        }

        throw new RuntimeException("Not supported operation.");
    }

    private static byte[] readClassAsResource(Class<?> clazz) throws IOException {
        return ByteClassLoaderUtil.readClassBytesFromAsResource(AdminLauncher.class, resolvePath(clazz));
    }

    /**
     * "cc.blynk.MyClass" -> "/cc/blynk/MyClass.class"
     */
    private static String resolvePath(Class<?> clazz) {
        return "/" + clazz.getCanonicalName().replaceAll("\\.", "/") + ".class";
    }

    public void connect(String... args) throws IOException {
        try (Socket client = new Socket(host, port);
             DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            byte[] classBytes = loadClass(args[0]);

            sendParams(args, outToServer);

            //send executable class itself
            outToServer.writeShort(classBytes.length);
            outToServer.write(classBytes);
            outToServer.flush();

            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                System.out.println(responseLine);
                if (responseLine.contains("ok")) {
                    break;
                }
            }
        }
    }

}
