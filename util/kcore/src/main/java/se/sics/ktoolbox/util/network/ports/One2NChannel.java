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
package se.sics.ktoolbox.util.network.ports;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortCoreHelper;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.id.Identifier;
import se.sics.kompics.network.MessageNotify;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class One2NChannel<P extends PortType> implements ChannelCore<P> {

  private final static Logger LOG = LoggerFactory.getLogger(One2NChannel.class);
  private String logPrefix = "";
  private String details = "";

  private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

  private volatile boolean destroyed = false;

  private final PortCore<P> sourcePort;
  // Use HashMap for now and switch to a more efficient datastructure if necessary
  private final Multimap<Identifier, PortCore<P>> nPorts = HashMultimap.create();
  private final ChannelIdExtractor<KompicsEvent, Identifier> channelSelector;
  private final ChannelFilter filter;

  private One2NChannel(String channelName, PortCore<P> sourcePort, ChannelIdExtractor<?, Identifier> channelSelector,
    ChannelFilter filter) {
    this.logPrefix = channelName + " ";
    this.sourcePort = sourcePort;
    this.channelSelector = (ChannelIdExtractor<KompicsEvent, Identifier>) channelSelector;
    this.filter = filter;
    StringBuilder detailsSB = new StringBuilder();
    detailsSB.append("type:").append(sourcePort.getPortType().getClass().getName()).append(" ");
    detailsSB.append("owner:").append(sourcePort.getOwner().getComponent().getClass().getName()).append(" ");
    details = detailsSB.toString();
    LOG.info("{}created:{}", logPrefix, details);
  }

  @Override
  public boolean isDestroyed() {
    return destroyed;
  }

  @Override
  public P getPortType() {
    return sourcePort.getPortType();
  }

  @Override
  public boolean hasPositivePort(Port<P> port) {
    return hasPort(port, true);
  }

  @Override
  public boolean hasNegativePort(Port<P> port) {
    return hasPort(port, false);
  }

  private boolean hasPort(Port<P> port, boolean positive) {
    rwlock.readLock().lock();
    try {
      if (destroyed) {
        return false;
      }
      //TODO Alex - check if you can use bitwise XOR as there is no boolean XOR in java(aka ^^)
      if (PortCoreHelper.isPositive(sourcePort) ^ positive) {
        return nPorts.containsValue(port);
      } else {
        return sourcePort == port;
      }
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public void forwardToPositive(KompicsEvent event, int wid) {
    forwardTo(event, wid, true);
  }

  @Override
  public void forwardToNegative(KompicsEvent event, int wid) {
    forwardTo(event, wid, false);
  }

  private void forwardTo(KompicsEvent event, int wid, boolean positive) {
    Identifier overlayId;

    if (filter.filter(event)) {
      LOG.debug("{}filtered:{}", logPrefix, event);
      return;
    }
    if (channelSelector.getEventType().isAssignableFrom(event.getClass())) {
      overlayId = channelSelector.getValue(event);
      if (overlayId == null) {
        LOG.info("{}event:{} not processable in:{}", new Object[]{logPrefix, event, details});
        return;
      }
    } else if (event instanceof MessageNotify.Req && channelSelector.getEventType().isAssignableFrom(
      (((MessageNotify.Req) event).msg).getClass())) {
      overlayId = channelSelector.getValue(event);
      if (overlayId == null) {
        LOG.info("{}traffic not processable in:{}", logPrefix, details);
        return;
      }
    } else {
      LOG.info("{}cannot extract id for:{} from:{} in:{}",
        new Object[]{logPrefix, channelSelector.getEventType().getName(), event.getClass().getName(), details});
      return;
    }

    rwlock.readLock().lock();
    try {
      if (destroyed) {
        return;
      }
      //TODO Alex - check if you can use bitwise XOR as there is no boolean XOR in java(aka ^^)
      if (PortCoreHelper.isPositive(sourcePort) ^ positive) {
        if (!nPorts.containsKey(overlayId)) {
          LOG.info("{}no {} connection available for id:{} event:{} in:{}",
            new Object[]{logPrefix, (positive ? "positive" : "negative"), overlayId,
              event, details});
          return;
        }
        for (PortCore<P> port : nPorts.get(overlayId)) {
          port.doTrigger(event, wid, this);
        }
      } else {
        sourcePort.doTrigger(event, wid, this);
      }
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public void disconnect() {
    rwlock.writeLock().lock();
    try {
      if (destroyed) {
        return;
      }
      destroyed = true;
      sourcePort.removeChannel(this);
      for (Positive<P> port : nPorts.values()) {
        port.removeChannel(this);
      }
      nPorts.clear();
    } finally {
      rwlock.writeLock().unlock();
    }
  }

  public void addChannel(Identifier channelId, Negative<P> endPoint) {
    LOG.info("{}adding channel for:{} in:{}", new Object[]{logPrefix, channelId, details});
    add(channelId, (PortCore) endPoint);
  }

  public void addChannel(Identifier channelId, Positive<P> endPoint) {
    LOG.info("{}adding channel for:{} in:{}", new Object[]{logPrefix, channelId, details});
    add(channelId, (PortCore) endPoint);
  }

  private void add(Identifier channelId, PortCore<P> endPoint) {
    boolean endPointType = PortCoreHelper.isPositive(endPoint);
    boolean sourcePortType = PortCoreHelper.isPositive(sourcePort);
    rwlock.writeLock().lock();
    try {
      if (destroyed) {
        return;
      }
      //TODO Alex java XOR
      if (!(endPointType ^ sourcePortType)) {
        throw new RuntimeException("connecting the wrong end");
      }
      LOG.info("{}adding {} connection overlay:{} to:{} in:{}",
        new Object[]{logPrefix, (endPointType ? "positive" : "negative"), channelId,
          endPoint.getOwner().getComponent().getClass().getName(), details});
      nPorts.put(channelId, endPoint);
      endPoint.addChannel(this);
    } finally {
      rwlock.writeLock().unlock();
    }

  }

  public void removeChannel(Identifier channelId, Positive<P> port) {
    remove(channelId, (PortCore<P>) port);
  }

  public void removeChannel(Identifier channelId, Negative<P> port) {
    remove(channelId, (PortCore<P>) port);
  }

  private void remove(Identifier channelId, PortCore<P> port) {
    rwlock.writeLock().lock();
    try {
      if (destroyed) {
        return;
      }
      nPorts.remove(channelId, port);
      port.removeChannel(this);
    } finally {
      rwlock.writeLock().unlock();
    }
  }

  public static <P extends PortType> One2NChannel<P> getChannel(String channelName, Negative<P> sourcePort,
    ChannelIdExtractor<?, Identifier> channelSelector) {
    One2NChannel<P> one2NC = new One2NChannel(channelName, (PortCore) sourcePort, channelSelector,
      NoChannelFilter.instance);
    sourcePort.addChannel(one2NC);
    return one2NC;
  }

  public static <P extends PortType> One2NChannel<P> getChannel(String channelName, Positive<P> sourcePort,
    ChannelIdExtractor<?, Identifier> channelSelector) {
    One2NChannel<P> one2NC = new One2NChannel(channelName, (PortCore) sourcePort, channelSelector,
      NoChannelFilter.instance);
    sourcePort.addChannel(one2NC);
    return one2NC;
  }

  public static <P extends PortType> One2NChannel<P> getChannel(String channelName,
    Negative<P> sourcePort, ChannelIdExtractor<?, Identifier> channelSelector, ChannelFilter filter) {
    One2NChannel<P> one2NC = new One2NChannel(channelName, (PortCore) sourcePort, channelSelector, filter);
    sourcePort.addChannel(one2NC);
    return one2NC;
  }

  public static <P extends PortType> One2NChannel<P> getChannel(String channelName,
    Positive<P> sourcePort, ChannelIdExtractor<?, Identifier> channelSelector, ChannelFilter filter) {
    One2NChannel<P> one2NC = new One2NChannel(channelName, (PortCore) sourcePort, channelSelector, filter);
    sourcePort.addChannel(one2NC);
    return one2NC;
  }
}
