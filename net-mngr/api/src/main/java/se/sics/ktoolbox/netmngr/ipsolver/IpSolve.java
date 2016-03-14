/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.ktoolbox.netmngr.ipsolver;

import java.net.InetAddress;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import se.sics.ktoolbox.netmngr.ipsolver.util.IpAddressStatus;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpSolve {

    public static class Request extends se.sics.kompics.Direct.Request {

        public final EnumSet<NetworkInterfacesMask> netInterfaces;

        public Request(EnumSet<NetworkInterfacesMask> netInterfaces) {
            super();
            this.netInterfaces = netInterfaces;
        }
        
        public Response answer(List<IpAddressStatus> addrs, InetAddress boundIp) {
            return new Response(netInterfaces, addrs, boundIp);
        }
    }

    public static class Response implements se.sics.kompics.Direct.Response {

        public final EnumSet<NetworkInterfacesMask> netInterfaces;
        public final List<IpAddressStatus> addrs;
        public final InetAddress boundIp;

        Response(EnumSet<NetworkInterfacesMask> netInterfaces, List<IpAddressStatus> addrs, InetAddress boundIp) {
            assert addrs != null;

            this.netInterfaces = netInterfaces;
            this.addrs = addrs;
            this.boundIp = boundIp;
        }

        public boolean hasIpAddrStatus() {
            return !addrs.isEmpty();
        }

        /**
         * @param number the network interface number, starting at 1.
         * @return
         */
        public InetAddress getTenDotIpAddress(int number) {
            InetAddress addr = null;
            Iterator<IpAddressStatus> iter = addrs.iterator();
            int i = 0;
            while (iter.hasNext() && i < number) {
                addr = iter.next().getAddr();
                String textualPrefixAddr = addr.getHostAddress();
                int firstDot = textualPrefixAddr.indexOf(".");
                if (firstDot > 0) {
                    textualPrefixAddr = textualPrefixAddr.substring(0, firstDot);
                }
                if (textualPrefixAddr.compareTo("10") == 0) {
                    i++;
                } else {
                    addr = null;
                }
            }
            return addr;
        }
    }

    public static enum Protocol {

        UDP, TCP, NONE_SPECIFIED
    };

    public static enum NetworkInterfacesMask {

        PRIVATE,
        TEN_DOT_PRIVATE,
        LOOPBACK,
        PUBLIC,
        ALL
    };
}
