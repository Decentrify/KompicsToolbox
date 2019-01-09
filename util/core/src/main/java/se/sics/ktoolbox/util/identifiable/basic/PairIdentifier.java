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
package se.sics.ktoolbox.util.identifiable.basic;

import java.util.Objects;
import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PairIdentifier<A extends Identifier,B extends Identifier> implements Identifier {
  public final A id1;
  public final B id2;
  
  public PairIdentifier(A id1, B id2) {
    this.id1 = id1;
    this.id2 = id2;
  }

  @Override
  public int partition(int nrPartitions) {
    if(nrPartitions > Integer.MAX_VALUE / 2) {
      throw new RuntimeException("fix this");
    }
    int partition = (id1.partition(nrPartitions) + id2.partition(nrPartitions)) % nrPartitions;
    return partition;
  }

  @Override
  public int compareTo(Identifier o) {
    int result;
    PairIdentifier that = (PairIdentifier) o;
    result = this.id1.compareTo(that.id1);
    if(result == 0) {
      result = this.id2.compareTo(that.id2);
    }
    return result;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 19 * hash + Objects.hashCode(this.id1);
    hash = 19 * hash + Objects.hashCode(this.id2);
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
    final PairIdentifier<?, ?> other = (PairIdentifier<?, ?>) obj;
    if (!Objects.equals(this.id1, other.id1)) {
      return false;
    }
    if (!Objects.equals(this.id2, other.id2)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "<" + id1 + "," + id2 + ">";
  }
}
