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
package se.sics.ktoolbox.util.network.basic;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;
import se.sics.kompics.network.Address;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicAddress implements KAddress {

  private final Identifier id;
  private final String printForm;
  private final InetSocketAddress isa;

  public BasicAddress(InetAddress adr, int port, Identifier id) {
    this.isa = new InetSocketAddress(adr, port);
    this.id = id;
    this.printForm = adr.getHostAddress() + ":" + port + "<" + id + ">";
  }

  @Override
  public InetAddress getIp() {
    return this.isa.getAddress();
  }

  @Override
  public int getPort() {
    return this.isa.getPort();
  }

  @Override
  public KAddress withPort(int port) {
    return new BasicAddress(isa.getAddress(), port, id);
  }

  @Override
  public InetSocketAddress asSocket() {
    return this.isa;
  }

  @Override
  public boolean sameHostAs(Address other) {
    if (other == null) {
      return false;
    }
    if (!this.isa.equals(other.asSocket())) {
      return false;
    }
    return true;
  }

  @Override
  public Identifier getId() {
    return id;
  }

  @Override
  public String toString() {
    return printForm;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 41 * hash + Objects.hashCode(this.id);
    hash = 41 * hash + Objects.hashCode(this.printForm);
    hash = 41 * hash + Objects.hashCode(this.isa);
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
    final BasicAddress other = (BasicAddress) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    if (!Objects.equals(this.printForm, other.printForm)) {
      return false;
    }
    if (!Objects.equals(this.isa, other.isa)) {
      return false;
    }
    return true;
  }
}
