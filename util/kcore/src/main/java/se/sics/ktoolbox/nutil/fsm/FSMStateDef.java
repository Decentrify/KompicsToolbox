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
package se.sics.ktoolbox.nutil.fsm;

import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.nutil.fsm.ids.FSMStateDefId;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMStateDef {
  private FSMStateDefId id = null;
  //Class is a FSMEvent subtype
  private final Map<Class, FSMEventHandler> handlers = new HashMap<>(); 
  private boolean sealed = false;
  
  //set upon registration with FSMDef
  void setId(FSMStateDefId setId) throws FSMException {
    if(id != null) {
      throw new FSMException("double use of state def within the same fsm - not allowed");
    }
    this.id = setId;
  }
  
  public FSMStateDefId getId() {
    return id;
  }
  
  public void register(Class<? extends FSMEvent> event, FSMEventHandler handler) throws FSMException {
    if(sealed) {
      throw new FSMException("Trying to register handler after definition has been sealed");
    }
    if(handlers.containsKey(event)) {
      throw new FSMException("Handler already registered for event:" + event);
    }
    handlers.put(event, handler);
  }
  
  public void seal() {
    sealed = true;
  }
  
  protected FSMState build(Identifier baseId, FSMExternalState es, FSMInternalState is) throws FSMException {
    if(!sealed) {
      throw new FSMException("trying to build an unsealed definition");
    }
    return new FSMState(id.getStateId(baseId), es, is, handlers);
  }
}
