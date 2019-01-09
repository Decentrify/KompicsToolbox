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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.network.portsv2.SelectableEventV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TimerProxyImpl implements TimerProxy {

  private Logger logger;
  private Positive<Timer> timer;
  private ComponentProxy proxy;
  private Map<UUID, Consumer<Boolean>> periodicCallbacks = new HashMap<>();
  private Map<UUID, Consumer<Boolean>> oneTimeCallbacks = new HashMap<>();
  private final Identifier timerProxyId;

  public TimerProxyImpl() {
    this(null);
  }

  public TimerProxyImpl(Identifier timerProxyId) {
    this.timerProxyId = timerProxyId;
  }

  @Override
  public TimerProxy setup(ComponentProxy proxy, Logger logger) {
    this.proxy = proxy;
    this.logger = logger;
    timer = proxy.getNegative(Timer.class).getPair();
    proxy.subscribe(handleTimeout, timer);
    return this;
  }

  @Override
  public void cancel() {
    periodicCallbacks.keySet().forEach((timeoutId) -> cancelPeriodicTimer(timeoutId));
    periodicCallbacks.clear();
    oneTimeCallbacks.keySet().forEach((timeoutId) -> cancelTimer(timeoutId));
    oneTimeCallbacks.clear();
  }

  @Override
  public UUID scheduleTimer(long delay, Consumer<Boolean> callback) {
    ScheduleTimeout spt = new ScheduleTimeout(delay);
    Timeout t = new Timeout(spt, timerProxyId);
    spt.setTimeoutEvent(t);
    oneTimeCallbacks.put(t.getTimeoutId(), callback);
    proxy.trigger(spt, timer);
    return t.getTimeoutId();
  }

  @Override
  public void cancelTimer(UUID timeoutId) {
    CancelTimeout cpt = new CancelTimeout(timeoutId);
    proxy.trigger(cpt, timer);
    oneTimeCallbacks.remove(timeoutId);
  }

  @Override
  public UUID schedulePeriodicTimer(long delay, long period, Consumer<Boolean> callback) {
    SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(delay, period);
    Timeout t = new Timeout(spt, timerProxyId);
    spt.setTimeoutEvent(t);
    periodicCallbacks.put(t.getTimeoutId(), callback);
    proxy.trigger(spt, timer);
    return t.getTimeoutId();
  }

  @Override
  public void cancelPeriodicTimer(UUID timeoutId) {
    CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(timeoutId);
    proxy.trigger(cpt, timer);
    periodicCallbacks.remove(timeoutId);
  }

  Handler handleTimeout = new Handler<Timeout>() {
    @Override
    public void handle(Timeout t) {
      Consumer<Boolean> callback;
      callback = periodicCallbacks.get(t.getTimeoutId());
      if (callback != null) {
        logger.trace("periodic timer");
        callback.accept(true);
        return;
      }
      callback = oneTimeCallbacks.get(t.getTimeoutId());
      if (callback != null) {
        logger.trace("onetime timer");
        callback.accept(true);
        return;
      }
    }
  };

  public static class Timeout extends se.sics.kompics.timer.Timeout implements SelectableEventV2 {

    public static final String EVENT_TYPE = "TIMEOUT";
    private final Identifier timerProxyId;

    public Timeout(SchedulePeriodicTimeout spt, Identifier timerProxyId) {
      super(spt);
      this.timerProxyId = timerProxyId;
    }

    public Timeout(ScheduleTimeout st, Identifier timerProxyId) {
      super(st);
      this.timerProxyId = timerProxyId;
    }

    @Override
    public String toString() {
      return "Timeout{" + getTimeoutId() + (timerProxyId == null ? "" : ("," + timerProxyId.toString())) + '}';
    }

    @Override
    public String eventType() {
      return EVENT_TYPE;
    }

    public Identifier timerProxyId() {
      return timerProxyId;
    }
  }
}
