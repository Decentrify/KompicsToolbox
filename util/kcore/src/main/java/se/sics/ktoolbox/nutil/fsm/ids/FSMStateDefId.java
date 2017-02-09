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
import se.sics.ktoolbox.util.identifiable.Identifier;


/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMStateDefId {
  public final byte[] fsmDefId;
  public final byte[] stateId;
  
  public FSMStateDefId(byte[] fsmDefId, byte[] stateId) {
    this.fsmDefId = fsmDefId;
    this.stateId = stateId;
  }
  
  public FSMStateId getStateId(Identifier baseId) {
    return new FSMStateId(fsmDefId, stateId, baseId);
  }

  @Override
  public String toString() {
    return "<fsm,md:" + BaseEncoding.base16().encode(fsmDefId) +",sd:" + BaseEncoding.base16().encode(stateId) + ">";
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 37 * hash + Arrays.hashCode(this.fsmDefId);
    hash = 37 * hash + Arrays.hashCode(this.stateId);
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
    final FSMStateDefId other = (FSMStateDefId) obj;
    if (!Arrays.equals(this.fsmDefId, other.fsmDefId)) {
      return false;
    }
    if (!Arrays.equals(this.stateId, other.stateId)) {
      return false;
    }
    return true;
  }
}
