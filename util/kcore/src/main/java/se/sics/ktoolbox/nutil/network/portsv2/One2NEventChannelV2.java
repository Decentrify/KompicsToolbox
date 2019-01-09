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
package se.sics.ktoolbox.nutil.network.portsv2;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortCoreHelper;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class One2NEventChannelV2<P extends PortType> implements ChannelCore<P> {

  private final Logger logger;
  private final String details;
  private final String channelName;

  private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

  private volatile boolean destroyed = false;
  // These are supposed to be immutable after channel creation
  private final PortCore<P> sourcePort;
  //both on the multi end
  private final EventTypeExtractorV2 eventTypeExtractor;
  private final Map<String, EventIdExtractorV2> channelSelectors;
  // These can change during the lifetime of a channel
  // Use HashMap for now and switch to a more efficient datastructure if necessary
  private final Multimap<Identifier, PortCore<P>> nPorts = HashMultimap.create();

  public One2NEventChannelV2(String channelName, String details, Logger logger, PortCore<P> sourcePort,
    EventTypeExtractorV2 eventTypeExtractor, Map<String, EventIdExtractorV2> channelSelectors) {
    this.channelName = channelName;
    this.details = details;
    this.sourcePort = sourcePort;
    this.logger = logger;
    this.eventTypeExtractor = eventTypeExtractor;
    this.channelSelectors = channelSelectors;
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
    //TODO Alex - check if you can use bitwise XOR as there is no boolean XOR in java(aka ^^)
    boolean oneEnd = !(PortCoreHelper.isPositive(sourcePort) ^ positive);

    rwlock.readLock().lock();
    try {
      if (destroyed) {
        return false;
      }
      if (oneEnd) {
        return sourcePort == port;
      } else {
        return nPorts.containsValue(port);
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
    //TODO Alex - check if you can use bitwise XOR as there is no boolean XOR in java(aka ^^)
    boolean oneEnd = !(PortCoreHelper.isPositive(sourcePort) ^ positive);
    Optional<String> eventType = Optional.empty();
    Identifier channelId = null;
    if (!oneEnd) {
      if (!(event instanceof SelectableEventV2)) {
        eventType = Optional.empty();
      } else {
        SelectableEventV2 se = (SelectableEventV2)event;
        eventType = eventTypeExtractor.type(se);
        if (eventType.isPresent()) {
          EventIdExtractorV2 idExtractor = channelSelectors.get(eventType.get());
          if (idExtractor != null) {
            channelId = idExtractor.getValue(se);
          }
        }
      }
      if(!eventType.isPresent()) {
        logger.debug("{} port:{} event:{} filtered", new Object[]{channelName, details, event});
        return;
      }
      if(channelId == null) {
        logger.debug("{} port:{} event:{} without an id extractor", new Object[]{channelName, details, event});
        return;
      }
    }

    rwlock.readLock().lock();
    try {
      if (destroyed) {
        return;
      }
      if (oneEnd) {
        sourcePort.doTrigger(event, wid, this);
        return;
      }
      Collection<PortCore<P>> channelPorts = nPorts.get(channelId);
      if(channelPorts.isEmpty()) {
        logger.debug("{} port:{} event:{} without an id extractor", new Object[]{channelName, details, event});
        return;
      }
      if (!nPorts.containsKey(channelId)) {
        logger.debug("{} port:{} event:{} no {} connection for id:{}",
          new Object[]{channelName, details, event, (positive ? "positive" : "negative"), channelId});
        return;
      }
      for (PortCore<P> port : nPorts.get(channelId)) {
        port.doTrigger(event, wid, this);
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
    logger.info("{}adding channel for:{} in:{}", new Object[]{channelName, channelId, details});
    add(channelId, (PortCore) endPoint);
  }

  public void addChannel(Identifier channelId, Positive<P> endPoint) {
    logger.info("{}adding channel for:{} in:{}", new Object[]{channelName, channelId, details});
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
      logger.info("{}adding {} connection overlay:{} to:{} in:{}",
        new Object[]{logger, (endPointType ? "positive" : "negative"), channelId,
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

  public static <P extends PortType> One2NEventChannelV2<P> getChannel(String channelName, Logger logger,
    Negative<P> sourcePort, EventTypeExtractorV2 filter, Map<String, EventIdExtractorV2> channelSelectors) {
    StringBuilder detailsSB = new StringBuilder();
    detailsSB.append("type:").append(sourcePort.getPortType().getClass().getName()).append(" ");
    detailsSB.append("owner:").append(sourcePort.getOwner().getComponent().getClass().getName()).append(" ");
    String details = detailsSB.toString();
    logger.info("creating:{} details:{}", channelName, detailsSB);

    One2NEventChannelV2<P> one2NC = new One2NEventChannelV2(channelName, details, logger, 
      (PortCore) sourcePort, filter, channelSelectors);
    sourcePort.addChannel(one2NC);
    return one2NC;
  }

  public static <P extends PortType> One2NEventChannelV2<P> getChannel(String channelName, Logger logger,
    Positive<P> sourcePort, EventTypeExtractorV2 filter, Map<String, EventIdExtractorV2> channelSelectors) {
    StringBuilder detailsSB = new StringBuilder();
    detailsSB.append("type:").append(sourcePort.getPortType().getClass().getName()).append(" ");
    detailsSB.append("owner:").append(sourcePort.getOwner().getComponent().getClass().getName()).append(" ");
    String details = detailsSB.toString();
    logger.info("creating:{} details:{}", channelName, detailsSB);

    One2NEventChannelV2<P> one2NC = new One2NEventChannelV2(channelName, details, logger,
      (PortCore) sourcePort, filter, channelSelectors);
    sourcePort.addChannel(one2NC);
    return one2NC;
  }
}
