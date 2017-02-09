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
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMStateId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMachine {

  private final static Logger LOG = LoggerFactory.getLogger(FSMachine.class);
  private String logPrefix = "";

  public final FSMId id;
  private FSMState currentState;
  private final Map<FSMStateId, FSMState> states;
  private final Map<FSMTransition, Pair<FSMStateId, FSMStateId>> transitionTable;

  public FSMachine(FSMId id, Map<FSMStateId, FSMState> states, Map<FSMTransition, Pair<FSMStateId, FSMStateId>> transitionTable,
    FSMState initState) {
    this.id = id;
    this.states = states;
    this.transitionTable = transitionTable;
    this.currentState = initState;
    this.logPrefix = id.toString();
  }

  public boolean handle(FSMEvent event) throws FSMException {
    LOG.trace("{}state:{} handle event:{}", new Object[]{logPrefix, currentState.id, event});
    Optional<FSMTransition> transition = currentState.handle(event);
    if (!transition.isPresent()) {
      LOG.info("{}state:{} dropped event:{}", new Object[]{logPrefix, currentState.id, event});
      return false;
    }
    Pair<FSMStateId, FSMStateId> t = transitionTable.get(transition.get());
    if (t == null) {
      throw new FSMException("transition:" + transition.get() + " not defined");
    }
    //we can't check at definition the sanity of transitions
    if (!t.getValue0().equals(currentState.id)) {
      throw new FSMException("transition:" + transition.get() + " defined from:" + t.getValue0()
        + " to:" + t.getValue1() + " not possible from state:" + currentState.id);
    }
    //we check at definition that both from and to states of a transition are registered
    currentState = states.get(t.getValue1());
    LOG.trace("{}state:{} event:{} resulted in transition:{} to state:{}",
      new Object[]{logPrefix, currentState.id, event, transition.get(), t.getValue1()});
    return true;
  }
}
