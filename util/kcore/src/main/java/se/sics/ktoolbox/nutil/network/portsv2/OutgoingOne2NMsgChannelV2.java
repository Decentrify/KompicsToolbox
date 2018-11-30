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
import se.sics.kompics.Positive;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Network;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.network.KContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OutgoingOne2NMsgChannelV2 implements ChannelCore<Network> {

  private final Logger logger;
  private final String details;
  private final String channelName;

  private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

  private volatile boolean destroyed = false;
  // These are supposed to be immutable after channel creation
  private final Positive<Network> parentPort;
  //both on the multi end
  private final MsgTypeExtractorV2 channelFilter;
  private final Map<String, MsgIdExtractorV2> channelSelectors;
  // These can change during the lifetime of a channel
  // Use HashMap for now and switch to a more efficient datastructure if necessary
  private final Multimap<Identifier, Negative<Network>> childrenPorts = HashMultimap.create();

  public OutgoingOne2NMsgChannelV2(String channelName, String details, Logger logger, Positive<Network> sourcePort,
    MsgTypeExtractorV2 channelFilter, Map<String, MsgIdExtractorV2> channelSelectors) {
    this.channelName = channelName;
    this.details = details;
    this.parentPort = sourcePort;
    this.logger = logger;
    this.channelFilter = channelFilter;
    this.channelSelectors = channelSelectors;
  }

  @Override
  public boolean isDestroyed() {
    return destroyed;
  }

  @Override
  public Network getPortType() {
    return parentPort.getPortType();
  }

  @Override
  public boolean hasPositivePort(Port<Network> port) {
    return port == parentPort;
  }

  @Override
  public boolean hasNegativePort(Port<Network> port) {
    rwlock.readLock().lock();
    try {
      return childrenPorts.containsValue(port);
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public void forwardToPositive(KompicsEvent event, int wid) {
    rwlock.readLock().lock();
    try {
      if (!destroyed) {
        parentPort.doTrigger(event, wid, this);
      }
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public void forwardToNegative(KompicsEvent event, int wid) {
    if (event instanceof MessageNotify.Req || event instanceof MessageNotify.Resp) {
      throw new UnsupportedOperationException("not supporting notification for msgs");
    }
    KContentMsg msg = (KContentMsg) event;
    Optional<String> msgType;
    Identifier channelId = null;
      if (!(msg.getContent() instanceof SelectableMsgV2)) {
        msgType = Optional.empty();
      } else {
        SelectableMsgV2 sm = (SelectableMsgV2) msg.getContent();
        msgType = channelFilter.type(msg);
        if (msgType.isPresent()) {
          MsgIdExtractorV2 idExtractor = channelSelectors.get(msgType.get());
          if (idExtractor != null) {
            channelId = idExtractor.getValue(msg);
          }
        }
      }
      if (!msgType.isPresent()) {
        logger.debug("{} port:{} msg:{} filtered", new Object[]{channelName, details, msg});
        return;
      }
      if (channelId == null) {
        logger.debug("{} port:{} msg:{} without an id extractor", new Object[]{channelName, details, msg});
        return;
      }
    rwlock.readLock().lock();
    try {
      if (destroyed) {
        return;
      }

      Collection<Negative<Network>> channelPorts = childrenPorts.get(channelId);
      if (channelPorts.isEmpty()) {
        logger.debug("{} port:{} msg:{} without an id extractor", new Object[]{channelName, details, msg});
        return;
      }
      if (!childrenPorts.containsKey(channelId)) {
        logger.debug("{} port:{} msg:{} no connection for id:{}", new Object[]{channelName, details, msg, channelId});
        return;
      }
      for (Negative<Network> port : childrenPorts.get(channelId)) {
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
      parentPort.removeChannel(this);
      for (Negative<Network> port : childrenPorts.values()) {
        port.removeChannel(this);
      }
      childrenPorts.clear();
    } finally {
      rwlock.writeLock().unlock();
    }
  }

  public void addChannel(Identifier channelId, Negative<Network> endPoint) {
    rwlock.writeLock().lock();
    try {
      if (destroyed) {
        return;
      }
      logger.info("{} port:{} adding channel:{} to:{}",
        new Object[]{channelName, details, channelId, endPoint.getOwner().getComponent().getClass().getName()});
      childrenPorts.put(channelId, endPoint);
      endPoint.addChannel(this);
    } finally {
      rwlock.writeLock().unlock();
    }

  }

  public void removeChannel(Identifier channelId, Negative<Network> port) {
    rwlock.writeLock().lock();
    try {
      if (destroyed) {
        return;
      }
      childrenPorts.remove(channelId, port);
      port.removeChannel(this);
    } finally {
      rwlock.writeLock().unlock();
    }
  }

  public static OutgoingOne2NMsgChannelV2 getChannel(String channelName, Logger logger,
    Positive<Network> parentPort, MsgTypeExtractorV2 filter, Map<String, MsgIdExtractorV2> channelSelectors) {
    StringBuilder detailsSB = new StringBuilder();
    detailsSB.append("type:").append(parentPort.getPortType().getClass().getName()).append(" ");
    detailsSB.append("owner:").append(parentPort.getOwner().getComponent().getClass().getName()).append(" ");
    String details = detailsSB.toString();
    logger.info("creating:{} details:{}", channelName, detailsSB);

    OutgoingOne2NMsgChannelV2 one2NC = new OutgoingOne2NMsgChannelV2(channelName, details, logger,
      parentPort, filter, channelSelectors);
    parentPort.addChannel(one2NC);
    return one2NC;
  }
}
