package cc.blynk.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.03.16.
 */
public class IPUtils {

    private static final Logger log = LogManager.getLogger(IPUtils.class);

    public static String resolveHostIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.getDisplayName().startsWith("eth")) {
                    Enumeration<InetAddress> ips = networkInterface.getInetAddresses();
                    while (ips.hasMoreElements()) {
                        InetAddress inetAddress = ips.nextElement();
                        if (inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                    return networkInterface.getDisplayName();
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Problem resolving current host IP.", e.getMessage());
            return "127.0.0.1";
        }
    }

}
