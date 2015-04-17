/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.aggregator.example.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.aggregator.example.core.Peer;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(HostManagerComp.class);
    
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    
    private DecoratedAddress aggregatorNodeAddress;
    private DecoratedAddress selfAddress;
    
    public HostManagerComp(HostManagerInit init) {

        this.selfAddress = init.self;
        this.aggregatorNodeAddress = init.aggregatorComponent;

        Component peer = create(Peer.class, new Peer.PeerInit(5000, selfAddress, aggregatorNodeAddress));

        connect(peer.getNegative(Network.class), network);
        connect(peer.getNegative(Timer.class), timer);
    
        subscribe(handleStart, control);
    }
    
    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.debug("Host Manager Component Started.");
        }
        
    };

    
    public static class HostManagerInit extends Init<HostManagerComp> {

        public final long seed;
        public final DecoratedAddress self;
        public final DecoratedAddress aggregatorComponent;
        
        public HostManagerInit(long seed, DecoratedAddress self, DecoratedAddress aggregatorComponent) {
            this.seed = seed;
            this.self = self;
            this.aggregatorComponent = aggregatorComponent;
        }
    }
}
