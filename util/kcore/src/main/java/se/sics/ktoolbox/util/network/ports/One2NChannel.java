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
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * Using synchronized blocks instead of ReentrantReadWrite locks - doing low
 * amount of work in the blocks and RRWLocks are slow to obtain(talk to Lars)
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class One2NChannel<P extends PortType> implements ChannelCore<P> {

    private final static Logger LOG = LoggerFactory.getLogger(One2NChannel.class);
    private String logPrefix = " ";

    private volatile boolean destroyed = false;

    private final PortCore<P> sourcePort;
    // Use HashMap for now and switch to a more efficient datastructure if necessary
    private final Multimap<Identifier, PortCore<P>> nPorts = HashMultimap.create();
    private final ChannelIdExtractor<KompicsEvent, Identifier> channelSelector;

    private One2NChannel(PortCore<P> sourcePort, ChannelIdExtractor<?, Identifier> channelSelector) {
        this.sourcePort = sourcePort;
        this.channelSelector = (ChannelIdExtractor<KompicsEvent, Identifier>) channelSelector;
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
        synchronized (this) {
            if (destroyed) {
                return false;
            }
            //TODO Alex - check if you can use bitwise XOR as there is no boolean XOR in java(aka ^^)
            if (PortCoreHelper.isPositive(sourcePort) ^ positive) {
                return nPorts.containsValue(port);
            } else {
                return sourcePort == port;
            }
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
        if (!channelSelector.getEventType().isAssignableFrom(event.getClass())) {
            throw new RuntimeException("selector event type:" + channelSelector.getEventType()
                    + "should be a supertype of event type:" + event.getClass());
        }
        Identifier overlayId = channelSelector.getValue(event);
        
        synchronized (this) {
            if (destroyed) {
                return;
            }
            //TODO Alex - check if you can use bitwise XOR as there is no boolean XOR in java(aka ^^)
            if (PortCoreHelper.isPositive(sourcePort) ^ positive) {
                if (!nPorts.containsKey(overlayId)) {
                    LOG.warn("{}no {} connection available for overlay:{}",
                            new Object[]{logPrefix, (positive ? "positive" : "negative"), overlayId});
                }
                for (PortCore<P> port : nPorts.get(overlayId)) {
                    port.doTrigger(event, wid, this);
                }
            } else {
                sourcePort.doTrigger(event, wid, this);
            }
        }
    }

    @Override
    public void disconnect() {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            destroyed = true;
        }

        sourcePort.removeChannel(this);
        for (Positive<P> port : nPorts.values()) {
            port.removeChannel(this);
        }
        nPorts.clear();
    }
    
    public void addChannel(Identifier overlayId, Negative<P> endPoint) {
        add(overlayId, (PortCore)endPoint);
    }
    
    public void addChannel(Identifier overlayId, Positive<P> endPoint) {
        add(overlayId, (PortCore)endPoint);
    }

    private void add(Identifier overlayId, PortCore<P> endPoint) {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            //TODO Alex java XOR
            if(!(PortCoreHelper.isPositive(endPoint) ^ PortCoreHelper.isPositive(sourcePort))) {
                throw new RuntimeException("connecting the wrong end");
            }
            nPorts.put(overlayId, endPoint);
            endPoint.addChannel(this);
        }

    }

    public void removeChannel(Identifier overlayId, PortCore<P> endPoint) {
        synchronized (this) {
            if (destroyed) {
                return;
            }
            nPorts.remove(overlayId, endPoint);
            endPoint.removeChannel(this);
        }
    }
    
    public static <P extends PortType> One2NChannel<P> getChannel(Negative<P> sourcePort, ChannelIdExtractor<?, Identifier> channelSelector) {
        One2NChannel<P> one2NC = new One2NChannel((PortCore)sourcePort, channelSelector);
        sourcePort.addChannel(one2NC);
        return one2NC;
    }
    
    public static <P extends PortType> One2NChannel<P> getChannel(Positive<P> sourcePort, ChannelIdExtractor<?, Identifier> channelSelector) {
        One2NChannel<P> one2NC = new One2NChannel((PortCore)sourcePort, channelSelector);
        sourcePort.addChannel(one2NC);
        return one2NC;
    }
}
