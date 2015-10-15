/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
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

package se.sics.ktoolbox.ipsolver;

import com.google.common.base.Optional;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.javatuples.Pair;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ktoolbox.ipsolver.util.IpAddressStatus;
import se.sics.ktoolbox.ipsolver.util.IpHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpSolver {
    /**
     * @param prefferedIp
     * @param netInterfaces - order of types is important as it will check for ips in order of preferrence
     * @return
     * @throws SocketException 
     */
    public static Optional<InetAddress> getIp(Optional<String> prefferedIp, List<GetIp.NetworkInterfacesMask> netInterfaces) throws SocketException {
        Pair<Integer, InetAddress> result = Pair.with(netInterfaces.size(), null);
        
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            if (ni.isVirtual()) {
                continue;
            }
            
            for (InterfaceAddress ifaceAddr : ni.getInterfaceAddresses()) {
                InetAddress addr = ifaceAddr.getAddress();
                if (addr instanceof Inet6Address) {
                    continue; // ignore ipv6 addresses
                }
                if(prefferedIp.isPresent() && addr.getCanonicalHostName().equals(prefferedIp.get())) {
                    return Optional.of(addr);
                }
                for(int i = 0; i < result.getValue0(); i++) {
                    if(IpHelper.isType(addr, netInterfaces.get(i))) {
                        result = Pair.with(i, addr);
                        break;
                    }
                }
            }
        }
        return Optional.fromNullable(result.getValue1());
    }
    public static Set<IpAddressStatus> getLocalNetworkInterfaces(EnumSet<GetIp.NetworkInterfacesMask> netInterfaces) throws SocketException {
        Set<IpAddressStatus> addresses = new HashSet<IpAddressStatus>();

        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            if (ni.isVirtual()) {
                continue;
            }

            List<InterfaceAddress> ifaces = ni.getInterfaceAddresses();
            for (InterfaceAddress ifaceAddr : ifaces) {
                //TODO Alex - is this check true at any time?
                if (ifaceAddr == null) {
                    continue;
                }
                InetAddress addr = ifaceAddr.getAddress();
                // ignore ipv6 addresses
                if (addr instanceof Inet6Address) {
                    continue;
                }

                if (!IpHelper.filter(addr, netInterfaces)) {
                    int networkPrefixLength = ifaceAddr.getNetworkPrefixLength();
                    int mtu = ni.getMTU();
                    boolean isUp = ni.isUp();
                    IpAddressStatus ipAddr = new IpAddressStatus(ni, addr, isUp, networkPrefixLength, mtu);
                    addresses.add(ipAddr);
                }
            }
        }
        return addresses;
    }
}
