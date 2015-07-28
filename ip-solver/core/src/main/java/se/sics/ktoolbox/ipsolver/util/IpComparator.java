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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpComparator implements Comparator<IpAddressStatus> {

    private static final Logger LOG = LoggerFactory.getLogger("InternalLogic");

    @Override
    public int compare(IpAddressStatus obj1, IpAddressStatus obj2) {
        if (obj1 == null) {
            return -1;
        }
        if (obj2 == null) {
            return 1;
        }
        if (obj1 == obj2 || obj1.equals(obj2)) {
            return 0;
        }

        if (obj1.getAddr() instanceof Inet6Address) {
            return -1;
        }
        if (obj2.getAddr() instanceof Inet6Address) {
            return 1;
        }
        if (obj2.isUp() && !obj1.isUp()) {
            return -1;
        }
        if (obj1.isUp() && !obj2.isUp()) {
            return -1;
        }

        try {
            InetAddress loopbackIp = InetAddress.getByName("127.0.0.1");
            if (obj1.getAddr().equals(loopbackIp)) {
                return 1;
            } else if (obj2.getAddr().equals(loopbackIp)) {
                return -1;
            }

            InetAddress localIp = InetAddress.getByName("127.0.1.1");
            if (obj1.getAddr().equals(localIp)) {
                return 1;
            } else if (obj2.getAddr().equals(localIp)) {
                return -1;
            }
        } catch (UnknownHostException ex) {
            LOG.error("IpComparator - unrecognized loopback addresses - should not happen");
            throw new RuntimeException("unrecognized loopback addresses - should not happen", ex);
        }

        if (IpHelper.isPrivate(obj1.getAddr()) && !IpHelper.isPrivate(obj2.getAddr())) {
            return 1;
        } else if (!IpHelper.isPrivate(obj1.getAddr()) && IpHelper.isPrivate(obj2.getAddr())) {
            return -1;
        }
        if (IpHelper.isTenDot(obj1.getAddr())&& !IpHelper.isTenDot(obj2.getAddr())) {
            return 1;
        } else if (!IpHelper.isTenDot(obj1.getAddr()) && IpHelper.isTenDot(obj2.getAddr())) {
            return -1;
        }

        if (obj1.getMtu() > obj2.getMtu()) {
            return -1;
        } else {
            return 1;
        }
    }
};