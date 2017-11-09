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
package se.sics.ktoolbox.util.network.nat;

import java.util.Objects;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NatType {

  public final Nat.Type type;
  public final Nat.MappingPolicy mappingPolicy;
  public final Nat.AllocationPolicy allocationPolicy;
  public final int delta;
  public final Nat.FilteringPolicy filteringPolicy;
  public final long bindingTimeout;

  private NatType(Nat.Type type, Nat.MappingPolicy mappingPolicy, Nat.AllocationPolicy allocationPolicy, int delta,
    Nat.FilteringPolicy filteringPolicy, long bindingTimeout) {
    this.type = type;
    this.mappingPolicy = mappingPolicy;
    this.allocationPolicy = allocationPolicy;
    this.delta = delta;
    this.filteringPolicy = filteringPolicy;
    this.bindingTimeout = bindingTimeout;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 89 * hash + Objects.hashCode(this.type);
    hash = 89 * hash + Objects.hashCode(this.mappingPolicy);
    hash = 89 * hash + Objects.hashCode(this.allocationPolicy);
    hash = 89 * hash + this.delta;
    hash = 89 * hash + Objects.hashCode(this.filteringPolicy);
    hash = 89 * hash + (int) (this.bindingTimeout ^ (this.bindingTimeout >>> 32));
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NatType other = (NatType) obj;
    if (this.type != other.type) {
      return false;
    }
    if (this.mappingPolicy != other.mappingPolicy) {
      return false;
    }
    if (this.allocationPolicy != other.allocationPolicy) {
      return false;
    }
    if (this.delta != other.delta) {
      return false;
    }
    if (this.filteringPolicy != other.filteringPolicy) {
      return false;
    }
    if (this.bindingTimeout != other.bindingTimeout) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    switch (type) {
      case OPEN:
      case FIREWALL:
      case UDP_BLOCKED:
      case UPNP:
      case UNKNOWN:
      case NAT_OPEN_PORTS:
        return type.code;
      case NAT:
        return type.code + "_" + mappingPolicy.code + "_" + allocationPolicy.code + "_" + filteringPolicy.code;
      default:
        return "undefined";
    }
  }

  public boolean isSimpleNat() {
    return type.equals(Nat.Type.NAT)
      && allocationPolicy.equals(Nat.AllocationPolicy.PORT_PRESERVATION)
      && filteringPolicy.equals(Nat.FilteringPolicy.ENDPOINT_INDEPENDENT);
  }

  public boolean isOpen() {
    return type.equals(Nat.Type.OPEN);
  }

  public boolean isNatOpenPorts() {
    return type.equals(Nat.Type.NAT_OPEN_PORTS);
  }

  public boolean isBlocked() {
    return type.equals(Nat.Type.UDP_BLOCKED);
  }

  public static NatType open() {
    return new NatType(Nat.Type.OPEN, null, null, 0, null, 0);
  }

  public static NatType natOpenPorts() {
    return new NatType(Nat.Type.NAT_OPEN_PORTS, null, null, 0, null, 0);
  }

  public static NatType firewall() {
    return new NatType(Nat.Type.FIREWALL, null, null, 0, null, 0);
  }

  public static NatType udpBlocked() {
    return new NatType(Nat.Type.UDP_BLOCKED, null, null, 0, null, 0);
  }

  public static NatType upnp() {
    return new NatType(Nat.Type.UPNP, null, null, 0, null, 0);
  }

  public static NatType nated(Nat.MappingPolicy mappingPolicy, Nat.AllocationPolicy allocationPolicy, int delta,
    Nat.FilteringPolicy filteringPolicy, long bindingTimeout) {
    assert mappingPolicy != null;
    assert allocationPolicy != null;
    assert filteringPolicy != null;
    assert bindingTimeout > 0;
    return new NatType(Nat.Type.NAT, mappingPolicy, allocationPolicy, delta, filteringPolicy, bindingTimeout);
  }

  public static NatType unknown() {
    return new NatType(Nat.Type.UNKNOWN, null, null, 0, null, 0);
  }

  public static boolean isOpen(KAddress address) {
    if (address instanceof NatAwareAddress) {
      return Nat.Type.OPEN.equals(((NatAwareAddress) address).getNatType().type);
    } else {
      return true;
    }
  }

  public static boolean isNated(KAddress address) {
    if (address instanceof NatAwareAddress) {
      return Nat.Type.NAT.equals(((NatAwareAddress) address).getNatType().type);
    } else {
      return false;
    }
  }

  public static boolean isNatOpenPorts(KAddress address) {
    if (address instanceof NatAwareAddress) {
      return Nat.Type.NAT_OPEN_PORTS.equals(((NatAwareAddress) address).getNatType().type);
    } else {
      return false;
    }
  }

  public static boolean isUnknown(NatAwareAddress address) {
    if (address instanceof NatAwareAddress) {
      return Nat.Type.UNKNOWN.equals(((NatAwareAddress) address).getNatType().type);
    } else {
      return false;
    }
  }
}
