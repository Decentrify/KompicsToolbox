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
package se.sics.ktoolbox.nutil.timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.RingTimer.Container;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class RingTimer<C extends Container> {

  private final int windowSize;
  private final int maxTimeout;
  private final int ringSize;
  //****************************************************
  private final ArrayList<List<Timeout<C>>> ring;
  private final Map<Identifier, Timeout<C>> timeouts = new HashMap<>();
  private int ringPointer = 0;
  //your timeout will take between (giventimeout, givenTimeout + 1window)
  private final int timeoutShift = 1;

  public RingTimer(int windowSize, int maxTimeout) {
    this.windowSize = windowSize;
    this.maxTimeout = maxTimeout;
    ringSize = maxTimeout / windowSize + timeoutShift;
    ring = new ArrayList<>(ringSize);
    setupRing();
  }

  private void setupRing() {
    for (int i = 0; i < ringSize; i++) {
      ring.add(new LinkedList<>());
    }
  }

  public boolean setTimeout(long rto, C container) {
    if (rto > maxTimeout) {
      return false;
    }
    int window = ((int) (rto / windowSize) + timeoutShift + ringPointer) % ringSize;
    Timeout t = new Timeout(container);
    ring.get(window).add(t);
    timeouts.put(container.getId(), t);
    return true;
  }

  public Optional<C> cancelTimeout(Identifier containerId) {
    Timeout<C> t = timeouts.remove(containerId);
    if(t == null) {
      return Optional.empty();
    }
    t.cancelTimeout();
    return Optional.of(t.container);
  }
  
  public int getSize() {
    return timeouts.size();
  }

  public List<C> windowTick() {
    List<C> result = new LinkedList<>();
    List<Timeout<C>> registered = ring.get(ringPointer);
    for(Timeout<C> t: registered) {
      if(t.ongoing) {
        result.add(t.container);
        timeouts.remove(t.container.getId());
      }
    }
    registered.clear();
    ringPointer = (ringPointer + 1) % ringSize;
    return result;
  }

  private static class Timeout<C extends Container> {

    public final C container;
    private boolean ongoing;

    Timeout(C container) {
      this.container = container;
      ongoing = true;
    }

    public void cancelTimeout() {
      ongoing = false;
    }

    public boolean ongoing() {
      return ongoing;
    }
  }
  
  public static interface Container extends Identifiable {
  }
}
