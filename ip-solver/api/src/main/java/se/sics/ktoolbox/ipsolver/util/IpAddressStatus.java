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
import java.net.NetworkInterface;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpAddressStatus {

    private final NetworkInterface networkInterface;
    private final InetAddress addr;
    private final boolean up;
    private final int networkPrefixLength;
    private final int mtu;

    public IpAddressStatus(NetworkInterface networkInterface, InetAddress addr, 
            boolean up, int networkPrefixLength, int mtu) {
        this.networkInterface = networkInterface;
        this.addr = addr;
        this.up = up;
        this.networkPrefixLength = networkPrefixLength;
        this.mtu = mtu;
    }

    public IpAddressStatus(IpAddressStatus ipAddrStatus) {
        this.networkInterface = ipAddrStatus.getNetworkInterface();
        this.addr = ipAddrStatus.getAddr();
        this.up = ipAddrStatus.isUp();
        this.networkPrefixLength = ipAddrStatus.getNetworkPrefixLength();
        this.mtu = ipAddrStatus.getMtu();
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public int getMtu() {
        return mtu;
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getNetworkPrefixLength() {
        return networkPrefixLength;
    }

    public boolean isUp() {
        return up;
    }

    // ignore MTU and networkprefix length values
    //TODO Alex - should ignore up or not?
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IpAddressStatus other = (IpAddressStatus) obj;
        if (this.networkInterface != other.networkInterface && (this.networkInterface == null || !this.networkInterface.equals(other.networkInterface))) {
            return false;
        }
        if (this.addr != other.addr && (this.addr == null || !this.addr.equals(other.addr))) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.networkInterface != null ? this.networkInterface.hashCode() : 0);
        hash = 67 * hash + (this.addr != null ? this.addr.hashCode() : 0);
        return hash;
    }
    
    @Override
    public String toString() {
        return "NI:" + networkInterface.toString() + " addr:" + addr.toString() + " up:" + up + " mtu:" + mtu + " netPrefixLength:" + networkPrefixLength;
    }
}
