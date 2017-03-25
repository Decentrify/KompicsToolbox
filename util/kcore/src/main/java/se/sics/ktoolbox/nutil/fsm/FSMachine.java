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
import com.google.common.collect.Table;
import java.util.Map;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.nutil.fsm.api.FSMBasicStateNames;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMOnKillAction;
import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;

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
    this.currentState = Pair.with((FSMStateName) FSMBasicStateNames.START, states.get(FSMBasicStateNames.START));
  }

  public void handlePositive(FSMEvent event) throws FSMException {
    LOG.trace("{}state:{} handle event:{}", new Object[]{id, currentState.getValue0(), event});
    Optional<FSMStateName> next = currentState.getValue1().handlePositive(event);
    if (!next.isPresent()) {
      LOG.info("{}state:{} does not handle positive port event:{}", new Object[]{id, currentState.getValue0(), event});
      return;
    }
    handle(next.get(), event);
  }

  public void handleNegative(FSMEvent event) throws FSMException {
    LOG.trace("{}state:{} handle event:{}", new Object[]{id, currentState.getValue0(), event});
    Optional<FSMStateName> next = currentState.getValue1().handleNegative(event);
    if (!next.isPresent()) {
      LOG.info("{}state:{} does not handle negative port event:{}", new Object[]{id, currentState.getValue0(), event});
      return;
    }
    handle(next.get(), event);
  }

  private void handle(FSMStateName next, FSMEvent event) throws FSMException {
    if (FSMBasicStateNames.FINAL.equals(next)) {
      oka.kill(id);
      return;
    }
    //we can't check at definition the sanity or completion of transition table
    if (!transitionTable.contains(currentState.getValue0(), next)) {
      throw new FSMException("transition from:" + currentState.getValue0() + " to:" + next + " not defined");
    }
    LOG.trace("{}state:{} event:{} resulted in transition to state:{}",
      new Object[]{id, currentState.getValue0(), event, next});
    //we check at definition that both from and to states of a transition are registered
    currentState = Pair.with(next, states.get(next));
  }

  public void handlePositive(FSMEvent payload, KContentMsg<KAddress, KHeader<KAddress>, FSMEvent> msg) throws
    FSMException {
    LOG.trace("{}state:{} handle msg:{}", new Object[]{id, currentState.getValue0(), msg});
    Optional<FSMStateName> next = currentState.getValue1().handlePositive(payload, msg);
    if (!next.isPresent()) {
      LOG.info("{}state:{} does not handle positive network msg:{}", new Object[]{id, currentState.getValue0(), msg});
      return;
    }
    handle(next.get(), payload, msg);
  }
  
  public void handleNegative(FSMEvent payload, KContentMsg<KAddress, KHeader<KAddress>, FSMEvent> msg) throws
    FSMException {
    LOG.trace("{}state:{} handle msg:{}", new Object[]{id, currentState.getValue0(), msg});
    Optional<FSMStateName> next = currentState.getValue1().handleNegative(payload, msg);
    if (!next.isPresent()) {
      LOG.info("{}state:{} does not handle negative network msg:{}", new Object[]{id, currentState.getValue0(), msg});
      return;
    }
    handle(next.get(), payload, msg);
  }

  private void handle(FSMStateName next, FSMEvent payload, KContentMsg<KAddress, KHeader<KAddress>, FSMEvent> msg)
    throws FSMException {
    if (FSMBasicStateNames.FINAL.equals(next)) {
      oka.kill(id);
      return;
    }
    //we can't check at definition the sanity or completion of transition table
    if (!transitionTable.contains(currentState.getValue0(), next)) {
      throw new FSMException("transition from:" + currentState.getValue0() + " to:" + next + " not defined");
    }
    LOG.trace("{}state:{} msg:{} resulted in transition to state:{}",
      new Object[]{id, currentState.getValue0(), msg, next});
    //we check at definition that both from and to states of a transition are registered
    currentState = Pair.with(next, states.get(next));
  }
}
