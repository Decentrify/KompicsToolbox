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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ShortCircuitChannel<P extends PortType> implements ChannelCore<P> {

    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

    private volatile boolean destroyed = false;

    private final Positive<P> positivePort;
    private final Positive<P> posMidPort;
    private final TrafficSelector posSelector;
    private final Negative<P> negativePort;
    private final Negative<P> negMidPort;
    private final TrafficSelector negSelector;

    private ShortCircuitChannel(Positive<P> positivePort, Positive<P> posMidPort, TrafficSelector posSelector,
            Negative<P> negativePort, Negative<P> negMidPort, TrafficSelector negSelector) {
        this.positivePort = positivePort;
        this.posMidPort = posMidPort;
        this.posSelector = posSelector;
        this.negativePort = negativePort;
        this.negMidPort = negMidPort;
        this.negSelector = negSelector;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public P getPortType() {
        return positivePort.getPortType();
    }

    @Override
    public boolean hasPositivePort(Port<P> port) {
        rwlock.readLock().lock();
        try {
            if (destroyed) {
                return false;
            }
            return port == positivePort || port == posMidPort;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    @Override
    public boolean hasNegativePort(Port<P> port) {
        rwlock.readLock().lock();
        try {
            if (destroyed) {
                return false;
            }
            return port == negativePort || port == negMidPort;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    @Override
    public void forwardToPositive(KompicsEvent event, int wid) {
        boolean pass = posSelector.pass(event);
        rwlock.readLock().lock();
        try {
            if (destroyed) {
                return;
            }
            if (pass) {
                posMidPort.doTrigger(event, wid, this);
            } else {
                positivePort.doTrigger(event, wid, this);
            }
        } finally {
            rwlock.readLock().unlock();
        }
    }

    @Override
    public void forwardToNegative(KompicsEvent event, int wid) {
        boolean pass = negSelector.pass(event);
        rwlock.readLock().lock();
        try {
            if (destroyed) {
                return;
            }
            if (pass) {
                negMidPort.doTrigger(event, wid, this);
            } else {
                negativePort.doTrigger(event, wid, this);
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
            positivePort.removeChannel(this);
            posMidPort.removeChannel(this);
            negativePort.removeChannel(this);
            negMidPort.removeChannel(this);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public static <P extends PortType> ShortCircuitChannel<P> getChannel(
            Positive<P> positivePort, Positive<P> posMidPort, TrafficSelector posSelector,
            Negative<P> negativePort, Negative<P> negMidPort, TrafficSelector negSelector) {
        ShortCircuitChannel<P> channel = new ShortCircuitChannel(positivePort, posMidPort, posSelector, 
                negativePort, negMidPort, negSelector);
        positivePort.addChannel(channel);
        posMidPort.addChannel(channel);
        negativePort.addChannel(channel);
        negMidPort.addChannel(channel);
        return channel;
    }
}
