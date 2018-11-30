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
import se.sics.ktoolbox.util.network.KContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OutgoingSimpleMsgChannelV2 implements ChannelCore<Network> {

  private final Logger logger;
  private final String details;
  private final String channelName;

  private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

  private volatile boolean destroyed = false;
  // These are supposed to be immutable after channel creation
  private final Positive<Network> parentPort;
  private final Negative<Network> childPort;
  //both on the multi end
  private final MsgFilterV2 filter;

  private OutgoingSimpleMsgChannelV2(String channelName, String details, Logger logger, Positive<Network> parentPort,
    Negative<Network> childPort, MsgFilterV2 filter) {
    this.channelName = channelName;
    this.details = details;
    this.parentPort = parentPort;
    this.childPort = childPort;
    this.logger = logger;
    this.filter = filter;
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
    return port == childPort;
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
    if (event instanceof MessageNotify.Resp) {
      throw new UnsupportedOperationException("not supporting notification for msgs");
    }
    KContentMsg msg = (KContentMsg) event;
    boolean filterMsg;
    if (!(msg.getContent() instanceof SelectableMsgV2)) {
      //for legacy - do not filter old msgs that do not implement SelectableMsgV2(not filterable) - let them pass
      filterMsg = false;
    } else {
      SelectableMsgV2 sm = (SelectableMsgV2) msg.getContent();
      filterMsg = filter.filter(msg);
    }
    if (filterMsg) {
      logger.debug("{} port:{} msg:{} filtered", new Object[]{channelName, details, msg});
      return;
    }

    rwlock.readLock().lock();
    try {
      if (!destroyed) {
        childPort.doTrigger(event, wid, this);
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
      childPort.removeChannel(this);
    } finally {
      rwlock.writeLock().unlock();
    }
  }

  public static OutgoingSimpleMsgChannelV2 getChannel(String channelName, Logger logger,
    Positive<Network> parentPort, Negative<Network> childPort, MsgFilterV2 filter) {
    StringBuilder detailsSB = new StringBuilder();
    detailsSB.append("type:").append(parentPort.getPortType().getClass().getName()).append(" ");
    detailsSB.append("owner:").append(parentPort.getOwner().getComponent().getClass().getName()).append(" ");
    String details = detailsSB.toString();
    logger.info("creating:{} details:{}", channelName, detailsSB);

    OutgoingSimpleMsgChannelV2 one2NC = new OutgoingSimpleMsgChannelV2(channelName, details, logger, parentPort, childPort, filter);
    parentPort.addChannel(one2NC);
    return one2NC;
  }
}
