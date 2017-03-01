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
import se.sics.ktoolbox.nutil.fsm.api.FSMOnKillAction;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.nutil.fsm.api.FSMBasicStateNames;
import com.google.common.base.Optional;
import com.google.common.collect.Table;
import java.util.Map;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMachine {

  private final static Logger LOG = LoggerFactory.getLogger(FSMachine.class);

  public final FSMId id;
  private final FSMOnKillAction oka;
  private Pair<FSMStateName, FSMState> currentState;
  private final Map<FSMStateName, FSMState> states;
  private final Table<FSMStateName, FSMStateName, Boolean> transitionTable;

  public FSMachine(FSMId id, FSMOnKillAction oka, Map<FSMStateName, FSMState> states,
    Table<FSMStateName, FSMStateName, Boolean> transitionTable) {
    this.id = id;
    this.oka = oka;
    this.states = states;
    this.transitionTable = transitionTable;
    this.currentState = Pair.with((FSMStateName)FSMBasicStateNames.START, states.get(FSMBasicStateNames.START));
  }

  public void handle(FSMEvent event) throws FSMException {
    LOG.trace("{}state:{} handle event:{}", new Object[]{id, currentState.getValue0(), event});
    Optional<FSMStateName> next = currentState.getValue1().handle(event);
    if (!next.isPresent()) {
      LOG.info("{}state:{} does not handle event:{}", new Object[]{id, currentState.getValue0(), event});
      return;
    }
    if (FSMBasicStateNames.FINAL.equals(next.get())) {
      oka.kill(id);
      return;
    }
    //we can't check at definition the sanity or completion of transition table
    if (!transitionTable.contains(currentState.getValue0(), next.get())) {
      throw new FSMException("transition from:" + currentState.getValue0() + " to:" + next.get() + " not defined");
    }
    LOG.trace("{}state:{} event:{} resulted in transition to state:{}",
      new Object[]{id, currentState.getValue0(), event, next.get()});
    //we check at definition that both from and to states of a transition are registered
    currentState = Pair.with(next.get(), states.get(next.get()));
  }
}
