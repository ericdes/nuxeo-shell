/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.shell;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPHelper {

    private static final String p255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    private static final Pattern IpV4Pattern = Pattern.compile("^(?:" + p255 + "\\.){3}" + p255 + "$");

    private IPHelper() {
    }

    public static String findConnectIP(String host, String port) {

        Matcher ipMatcher = IpV4Pattern.matcher(host);
        if (ipMatcher.matches()) {
            return host; // already an @IP
        }

        List<InetAddress> ips = new ArrayList<InetAddress>();

        if ("localhost".equals(host)) {
            // find every IP bound to a nic
            try {
                Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                while (ifaces.hasMoreElements()) {
                    NetworkInterface nic = ifaces.nextElement();
                    Enumeration<InetAddress> addrs = nic.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress ip = addrs.nextElement();
                        if (ip instanceof Inet4Address) {
                            ips.add(ip);
                        }
                    }
                }
            } catch (SocketException e) {
                return host;
            }
        } else {
            try {
                ips.addAll(Arrays.asList(Inet4Address.getAllByName(host)));
            } catch (UnknownHostException e) {
                return host;
            }
        }
        int intPort = Integer.parseInt(port);

        // tests IPs
        for (InetAddress ip : ips) {
            Socket s;
            try {
                s = new Socket(ip, intPort);
                s.close();
                return ip.getHostAddress();
            } catch (IOException e) {
                // not the right one, let's continue
            }
        }
        return host;
    }

}
