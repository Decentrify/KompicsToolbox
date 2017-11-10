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

import com.google.common.base.Optional;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import se.sics.kompics.network.Address;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NatAwareAddressImpl implements NatAwareAddress {

  /**
   * present only if different than publicAdr(nated adr) or if local(self)
   */
  final Optional<BasicAddress> privateAdr;
  final BasicAddress publicAdr;
  final NatType natType;
  final List<BasicAddress> parents;

  private NatAwareAddressImpl(Optional<BasicAddress> privateAdr, BasicAddress publicAdr, NatType natType,
    List<BasicAddress> parents) {
    this.privateAdr = privateAdr;
    this.publicAdr = publicAdr;
    this.natType = natType;
    this.parents = parents;
  }

  public NatAwareAddressImpl(BasicAddress privateAdr, BasicAddress publicAdr, NatType natType,
    List<BasicAddress> parents) {
    this(Optional.fromNullable(privateAdr), publicAdr, natType, parents);
  }

  @Override
  public Optional<BasicAddress> getPrivateAdr() {
    return privateAdr;
  }

  @Override
  public BasicAddress getPublicAdr() {
    return publicAdr;
  }

  @Override
  public NatType getNatType() {
    return natType;
  }

  public List<BasicAddress> getParents() {
    return parents;
  }

  @Override
  public InetAddress getIp() {
    return (privateAdr.isPresent() ? privateAdr.get().getIp() : publicAdr.getIp());
  }

  @Override
  public int getPort() {
    return (privateAdr.isPresent() ? privateAdr.get().getPort() : publicAdr.getPort());
  }

  @Override
  public Identifier getId() {
    return publicAdr.getId();
  }

  @Override
  public KAddress withPort(int port) {
    if(privateAdr.isPresent()) {
      //TODO Alex - when time fix this
      throw new RuntimeException("not sure how to do this - maybe 2 ports?");
    }
    return new NatAwareAddressImpl(privateAdr, (BasicAddress)publicAdr.withPort(port), natType, parents);
  }

  @Override
  public InetSocketAddress asSocket() {
    return (privateAdr.isPresent() ? privateAdr.get().asSocket() : publicAdr.asSocket());
  }

  @Override
  public boolean sameHostAs(Address other) {
    return (privateAdr.isPresent() ? privateAdr.get().sameHostAs(other) : publicAdr.sameHostAs(other));
  }

  public NatAwareAddress changePublicPort(int publicPort) {
    return new NatAwareAddressImpl(privateAdr,
      new BasicAddress(publicAdr.getIp(), publicPort, publicAdr.getId()), natType, parents);
  }

  public NatAwareAddress withPrivateAddress(BasicAddress privateAdr) {
    return new NatAwareAddressImpl(privateAdr, publicAdr, natType, parents);
  }

  public NatAwareAddress changeParents(List<BasicAddress> parents) {
    return new NatAwareAddressImpl(privateAdr, publicAdr, natType, parents);
  }

  public static NatAwareAddressImpl open(BasicAddress address) {
    Optional<BasicAddress> privateAdr = Optional.absent();
    return new NatAwareAddressImpl(privateAdr, address, NatType.open(), new ArrayList<BasicAddress>());
  }
  
  public static NatAwareAddressImpl natForwardedPorts(BasicAddress address) {
    Optional<BasicAddress> privateAdr = Optional.absent();
    return new NatAwareAddressImpl(privateAdr, address, NatType.natPortForwarding(), new ArrayList<BasicAddress>());
  }

  public static NatAwareAddressImpl nated(BasicAddress privateAdr, BasicAddress publicAdr, NatType natType,
    List<BasicAddress> parents) {
    return new NatAwareAddressImpl(privateAdr, publicAdr, natType, parents);
  }

  public static NatAwareAddressImpl unknown(BasicAddress adr) {
    Optional<BasicAddress> privateAdr = Optional.absent();
    return new NatAwareAddressImpl(privateAdr, adr, NatType.unknown(), new ArrayList<BasicAddress>());
  }
  
  public static NatAwareAddressImpl adr(BasicAddress publicAdr, NatType natType) {
    Optional<BasicAddress> privateAdr = Optional.absent();
    List<BasicAddress> parents = new LinkedList<>(); 
    return new NatAwareAddressImpl(privateAdr, publicAdr, natType, parents);
  }
  
  @Override
  public String toString() {
    return publicAdr.toString() + ":" + natType;
  }
}
