package org.nuxeo.ecm.shell;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPHelper {

    private static final String p255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

    private static final Pattern IpV4Pattern = Pattern.compile("^(?:" + p255 +
            "\\.){3}" + p255 + "$");

    private static final String LOOPBACK = "127.0.0.1";

    private IPHelper() {
    }

    public static Collection<String> findCandidateIPs(String host) {
        Matcher ipMatcher = IpV4Pattern.matcher(host);
        if (ipMatcher.matches()) {
            // already an IPv4 addess
            return Collections.singleton(host);
        }
        try {
            List<String> candidateIPs = new LinkedList<String>();
            if ("localhost".equals(host)) {
                Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                while (ifaces.hasMoreElements()) {
                    NetworkInterface nic = ifaces.nextElement();
                    Enumeration<InetAddress> addrs = nic.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress ip = addrs.nextElement();
                        if (ip instanceof Inet4Address) {
                            candidateIPs.add(ip.getHostAddress());
                        }
                    }
                }
            } else {
                for (InetAddress ip : Inet4Address.getAllByName(host)) {
                    candidateIPs.add(ip.getHostAddress());
                }
            }
            // put 127.0.0.1 first
            if (candidateIPs.remove(LOOPBACK)) {
                candidateIPs.add(0, LOOPBACK);
            }
            return candidateIPs;
        } catch (IOException e) {
            return Collections.singleton(host);
        }
    }

}
