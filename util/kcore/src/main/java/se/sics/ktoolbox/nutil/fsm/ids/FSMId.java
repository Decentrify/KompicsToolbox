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
package se.sics.ktoolbox.nutil.fsm.ids;

import com.google.common.io.BaseEncoding;
import java.util.Arrays;
import java.util.Objects;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMId {
  public final byte[] fsmDefId;
  public final Identifier baseId;
  
  public FSMId(byte[] fsmdId, Identifier baseId) {
    this.fsmDefId = fsmdId;
    this.baseId = baseId;
  }
  
  public FSMStateId getInitStateId() {
    byte[] initId = new byte[1];
    initId[0] = 0;
    return new FSMStateId(fsmDefId, initId, baseId);
  }
  
  public FSMDefId getDefId() {
    return new FSMDefId(fsmDefId);
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 61 * hash + Arrays.hashCode(this.fsmDefId);
    hash = 61 * hash + Objects.hashCode(this.baseId);
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
    final FSMId other = (FSMId) obj;
    if (!Arrays.equals(this.fsmDefId, other.fsmDefId)) {
      return false;
    }
    if (!Objects.equals(this.baseId, other.baseId)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "<fsm,md:" + BaseEncoding.base16().encode(fsmDefId) +",bid:" + baseId + ">";
  }
}
