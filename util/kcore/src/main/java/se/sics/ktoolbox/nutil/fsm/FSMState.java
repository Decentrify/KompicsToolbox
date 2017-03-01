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

import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import com.google.common.base.Optional;
import java.util.Map;
import se.sics.ktoolbox.nutil.fsm.handler.FSMEventHandler;
import se.sics.ktoolbox.nutil.fsm.handler.FSMStateChangeHandler;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMState {

  private final FSMStateName state;
  private final FSMEventHandler fallback;
  private final Optional<FSMStateChangeHandler> onEntry;
  private final Optional<FSMStateChangeHandler> onExit;
  private final FSMExternalState es;
  private final FSMInternalState is;
  private final Map<Class, FSMEventHandler> handlers;

  public FSMState(FSMStateName state, FSMEventHandler fallback, Optional<FSMStateChangeHandler> onEntry,
    Optional<FSMStateChangeHandler> onExit, FSMExternalState es, FSMInternalState is,
    Map<Class, FSMEventHandler> handlers) {
    this.state = state;
    this.fallback = fallback;
    this.onEntry = onEntry;
    this.onExit = onExit;
    this.es = es;
    this.is = is;
    this.handlers = handlers;
  }

  public void onEntry(FSMStateName from) {
    if(onEntry.isPresent()) {
      onEntry.get().handle(from, state, es, is);
    }
  }
  
  public void onExit(FSMStateName to) {
    if(onExit.isPresent()) {
      onExit.get().handle(state, to, es, is);
    }
  }
  
  public Optional<FSMStateName> handle(FSMEvent event) {
    FSMEventHandler handler = handlers.get(event.getClass());
    if (handler == null) {
      fallback.handle(state, es, is, event);
      return Optional.absent();
    }
    FSMStateName next = handler.handle(state, es, is, event);
    return Optional.of(next);
  }
}
