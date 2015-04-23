package cc.blynk.server.core.administration;

import cc.blynk.common.utils.Config;
import cc.blynk.server.core.administration.actions.ActivityMonitor;
import cc.blynk.server.core.administration.actions.ResetPassword;
import cc.blynk.server.utils.ByteClassLoaderUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    public static void main(String[] args) throws IOException {
        try (Socket client = new Socket("localhost", 8777);
             DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
             DataInputStream in = new DataInputStream(client.getInputStream())) {

            byte[] classBytes = loadClass(args[0]);

            int paramsNumber = args.length - 1;
            outToServer.writeByte(paramsNumber);

            if (args.length > 1) {
                for (int i = 0; i < paramsNumber; i++) {
                    String param = args[i + 1];
                    outToServer.writeShort(param.length());
                    outToServer.write(param.getBytes(Config.DEFAULT_CHARSET));
                }
            }
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

    /**
     * Loads class from jar as byte array.
     *
     * @param action type of class to load.
     * @return class byte array
     */
    private static byte[] loadClass(String action) throws IOException {
        switch (action.toLowerCase()) {
            case "resetpassword" :
                return ByteClassLoaderUtil.readClassBytesFromAsResource(AdminLauncher.class, resolvePath(ResetPassword.class));
            case "quotausage" :
                return ByteClassLoaderUtil.readClassBytesFromAsResource(AdminLauncher.class, resolvePath(ActivityMonitor.class));
        }

        throw new RuntimeException("Not supported operation.");
    }

    /**
     * "cc.blynk.MyClass" -> "/cc/blynk/MyClass.class"
     */
    private static String resolvePath(Class<?> clazz) {
        return "/" + clazz.getCanonicalName().replaceAll("\\.", "/") + ".class";
    }

}
