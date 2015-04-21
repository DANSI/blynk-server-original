package cc.blynk.server.administration;

import cc.blynk.common.utils.Config;
import cc.blynk.server.administration.actions.ResetPassword;
import cc.blynk.server.utils.ByteClassLoaderUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
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
                System.out.println("Server: " + responseLine);
                if (responseLine.contains("ok")) {
                    break;
                }
            }


        }


    }

    private static byte[] loadClass(String action) throws IOException {
        switch (action) {
            case "resetPassword" :
                return ByteClassLoaderUtil.readClassBytesFromAsResource(AdminLauncher.class, resolvePath(ResetPassword.class));
        }

        throw new RuntimeException("Not supported operation.");
    }

    private static String resolvePath(Class<?> clazz) {
        return "/" + clazz.getCanonicalName().replaceAll("\\.", "/") + ".class";
    }

}
