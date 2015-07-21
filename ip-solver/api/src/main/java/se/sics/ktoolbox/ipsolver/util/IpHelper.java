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
package se.sics.ktoolbox.ipsolver.util;

import java.net.InetAddress;
import java.util.EnumSet;
import se.sics.ktoolbox.ipsolver.msg.GetIp;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpHelper {
    public static boolean filter(InetAddress addr, EnumSet<GetIp.NetworkInterfacesMask> filterSet) {
        if (isLoopback(addr)) {
            return filterSet.contains(GetIp.NetworkInterfacesMask.IGNORE_LOOPBACK);
        }
        if (isTenDot(addr)) {
            return filterSet.contains(GetIp.NetworkInterfacesMask.IGNORE_TEN_DOT_PRIVATE);
        }
        if (isPrivate(addr)) {
            return filterSet.contains(GetIp.NetworkInterfacesMask.IGNORE_PRIVATE);
        }
        //at this point we know the address is public, otherwise we would have returned already
        return filterSet.contains(GetIp.NetworkInterfacesMask.IGNORE_PUBLIC);
    }

    public static boolean isLoopback(InetAddress adr) {
        return adr.isLoopbackAddress() || getTwoDotPrefix(adr).equals("127.0");
    }

    public static boolean isTenDot(InetAddress adr) {
        return getOneDotPrefix(adr).equals("10");
    }

    public static boolean isPrivate(InetAddress adr) {
        return getTwoDotPrefix(adr).equals("192.168");
    }
    
    private static String getOneDotPrefix(InetAddress addr) {
        String textualPrefixAddr = addr.getHostAddress();
        int firstDot = textualPrefixAddr.indexOf(".");
        textualPrefixAddr = textualPrefixAddr.substring(0, firstDot);
        return textualPrefixAddr;
    }

    private static String getTwoDotPrefix(InetAddress addr) {
        String textualPrefixAddr = addr.getHostAddress();
        int firstDot = textualPrefixAddr.indexOf(".");
        int secondDot = textualPrefixAddr.indexOf(".", firstDot + 1);
        if (firstDot > 0) {
            textualPrefixAddr = textualPrefixAddr.substring(0, secondDot);
        }
        return textualPrefixAddr;
    }
}
