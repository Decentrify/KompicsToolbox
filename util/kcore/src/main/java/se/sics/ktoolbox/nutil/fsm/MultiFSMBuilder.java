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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.javatuples.Pair;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMIdExtractor;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalStateBuilder;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalStateBuilders;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnFSMExceptionAction;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class MultiFSMBuilder {

  public static class Machine {

    private List<Pair<Class, List<Class>>> positivePorts = new LinkedList<>();
    private List<Pair<Class, List<Class>>> negativePorts = new LinkedList<>();
    private Set<Class> events = new HashSet<>();

    private Machine() {
    }

    public Events events() {
      return new Events(this);
    }

    private void buildEvents(List<Pair<Class, List<Class>>> positivePorts, List<Pair<Class, List<Class>>> negativePorts,
      Set<Class> events) {
      this.positivePorts = positivePorts;
      this.negativePorts = negativePorts;
      this.events = events;
    }

    public MultiFSM buildMultiFSM(final FSMachineDef fsmd, OnFSMExceptionAction oexa, FSMExternalState es,
      FSMInternalStateBuilder builder) throws FSMException {

      FSMIdExtractor fsmIdExtractor = new FSMIdExtractor() {

        @Override
        public Optional<FSMId> fromEvent(FSMEvent event) throws FSMException {
          if (events.contains(event.getClass())) {
            return Optional.of(fsmd.id.getFSMId(event.getBaseId()));
          }
          return Optional.absent();
        }
      };

      Map<FSMDefId, FSMachineDef> fsmds = new HashMap<>();
      fsmds.put(fsmd.id, fsmd);

      FSMInternalStateBuilders builders = new FSMInternalStateBuilders();
      builders.register(fsmd.id, builder);

      MultiFSM multiFSM = new MultiFSM(oexa, fsmIdExtractor, fsmds, es, builders, positivePorts, negativePorts);
      return multiFSM;
    }
  }

  public static class Events {

    private final Machine parent;
    private final List<Pair<Class, List<Class>>> positivePorts = new LinkedList<>();
    private final List<Pair<Class, List<Class>>> negativePorts = new LinkedList<>();
    private final Set<Class> events = new HashSet<>();

    private Events(Machine parent) {
      this.parent = parent;
    }

    public PositivePort positivePort(Class potType) {
      return new PositivePort(this, potType);
    }

    public NegativePort negativePort(Class potType) {
      return new NegativePort(this, potType);
    }

    private void buildPositivePort(Class portType, List<Class> newEvents) {
      positivePorts.add(Pair.with(portType, newEvents));
      events.addAll(newEvents);
    }

    private void buildNegativePort(Class portType, List<Class> newEvents) {
      negativePorts.add(Pair.with(portType, newEvents));
      events.addAll(newEvents);
    }

    public Machine buildEvents() {
      parent.buildEvents(positivePorts, negativePorts, events);
      return parent;
    }
  }

  public static class PositivePort {

    final Events parent;
    final Class portType;
    final List<Class> events = new LinkedList<>();

    private PositivePort(Events parent, Class portType) {
      this.parent = parent;
      this.portType = portType;
    }

    public PositivePort event(Class eventType) {
      events.add(eventType);
      return this;
    }

    public Events buildPort() {
      parent.buildPositivePort(portType, events);
      return parent;
    }
  }

  public static class NegativePort {

    final Events parent;
    final Class portType;
    final List<Class> events = new LinkedList<>();

    private NegativePort(Events parent, Class portType) {
      this.parent = parent;
      this.portType = portType;
    }

    public NegativePort event(Class eventType) {
      events.add(eventType);
      return this;
    }

    public Events buildPort() {
      parent.buildNegativePort(portType, events);
      return parent;
    }
  }

  public static Machine instance() {
    return new Machine();
  }
}
