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
public class FSMDefId {
  public final byte[] fsmDefId;

  public FSMDefId(byte[] fsmDefId) {
    this.fsmDefId = fsmDefId;
  }
  
  public FSMId getFSMId(Identifier baseId) {
    return new FSMId(fsmDefId, baseId);
  }
  
  @Override
  public String toString() {
    return "<fsm,md:" + BaseEncoding.base16().encode(fsmDefId) + ">";
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 61 * hash + Arrays.hashCode(this.fsmDefId);
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
    final FSMDefId other = (FSMDefId) obj;
    if (!Arrays.equals(this.fsmDefId, other.fsmDefId)) {
      return false;
    }
    return true;
  }
}
