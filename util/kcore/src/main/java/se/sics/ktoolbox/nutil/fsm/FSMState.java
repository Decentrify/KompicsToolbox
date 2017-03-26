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

import com.google.common.base.Optional;
import java.util.Map;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.handler.FSMEventHandler;
import se.sics.ktoolbox.nutil.fsm.handler.FSMMsgHandler;
import se.sics.ktoolbox.nutil.fsm.handler.FSMStateChangeHandler;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMState {
  
  private final FSMStateName state;
  private final Optional<FSMStateChangeHandler> onEntry;
  private final Optional<FSMStateChangeHandler> onExit;
  private final FSMExternalState es;
  private final FSMInternalState is;
  
  private final Map<Class, FSMEventHandler> positiveHandlers;
  private final Map<Class, FSMEventHandler> negativeHandlers;
  
  private final Map<Class, FSMMsgHandler> positiveMsgHandlers;
  private final Map<Class, FSMMsgHandler> negativeMsgHandlers;
  
  public FSMState(FSMStateName state, Optional<FSMStateChangeHandler> onEntry, Optional<FSMStateChangeHandler> onExit,
    FSMExternalState es, FSMInternalState is,
    Map<Class, FSMEventHandler> positiveHandlers, Map<Class, FSMEventHandler> negativeHandlers,
    Map<Class, FSMMsgHandler> positiveMsgHandlers, Map<Class, FSMMsgHandler> negativeMsgHandlers) {
    this.state = state;
    this.onEntry = onEntry;
    this.onExit = onExit;
    this.es = es;
    this.is = is;
    this.positiveHandlers = positiveHandlers;
    this.negativeHandlers = negativeHandlers;
    this.positiveMsgHandlers = positiveMsgHandlers;
    this.negativeMsgHandlers = negativeMsgHandlers;
  }
  
  public void onEntry(FSMStateName from) {
    if (onEntry.isPresent()) {
      onEntry.get().handle(from, state, es, is);
    }
  }
  
  public void onExit(FSMStateName to) {
    if (onExit.isPresent()) {
      onExit.get().handle(state, to, es, is);
    }
  }
  
  public Optional<FSMStateName> handlePositive(FSMEvent event) throws FSMException {
    FSMEventHandler handler = positiveHandlers.get(event.getClass());
    if (handler == null) {
      return Optional.absent();
    }
    FSMStateName next = handler.handle(state, es, is, event);
    return Optional.of(next);
  }
  
  public Optional<FSMStateName> handleNegative(FSMEvent event) throws FSMException {
    FSMEventHandler handler = negativeHandlers.get(event.getClass());
    if (handler == null) {
      return Optional.absent();
    }
    FSMStateName next = handler.handle(state, es, is, event);
    return Optional.of(next);
  }
  
  public Optional<FSMStateName> handlePositive(FSMEvent payload, KContentMsg<KAddress, KHeader<KAddress>, FSMEvent> msg)
    throws
    FSMException {
    FSMMsgHandler handler = positiveMsgHandlers.get(payload.getClass());
    if (handler == null) {
      return Optional.absent();
    }
    FSMStateName next = handler.handle(state, es, is, payload, msg);
    return Optional.of(next);
  }
  
  public Optional<FSMStateName> handleNegative(FSMEvent payload, KContentMsg<KAddress, KHeader<KAddress>, FSMEvent> msg)
    throws
    FSMException {
    FSMMsgHandler handler = negativeMsgHandlers.get(payload.getClass());
    if (handler == null) {
      return Optional.absent();
    }
    FSMStateName next = handler.handle(state, es, is, payload, msg);
    return Optional.of(next);
  }
  
  public FSMStateName fallback(FSMEvent event, FSMEventHandler fallback) throws FSMException {
    return fallback.handle(state, es, is, event);
  }

  public FSMStateName fallback(FSMEvent payload, KContentMsg msg, FSMMsgHandler fallback) throws FSMException {
    return fallback.handle(state, es, is, payload, msg);
  }
}
