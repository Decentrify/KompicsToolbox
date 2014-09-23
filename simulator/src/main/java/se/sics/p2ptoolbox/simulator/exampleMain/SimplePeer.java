/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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

package se.sics.p2ptoolbox.simulator.exampleMain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimplePeer extends ComponentDefinition {
    
    private static final Logger log = LoggerFactory.getLogger(SimplePeer.class);
    
    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Negative<SimplePeerPort> myPort = provides(SimplePeerPort.class);
    
    private VodAddress self;
    private VodAddress partner;
    
    public SimplePeer(SimplePeerInit init) {
        log.debug("Starting simple peer");
        
        this.self = init.self;
        this.partner = init.partner;
        
        subscribe(handlePing, myPort);
        subscribe(handleNetPing, network);
        subscribe(handleNetPong, network);
    }
    
    private Handler<TestMsg.Ping> handlePing = new Handler<TestMsg.Ping>() {

        @Override
        public void handle(TestMsg.Ping event) {
            log.debug("{} received local ping", self);
            trigger(new NetTestMsg.Ping(self, partner), network);
        }
    };
    
    private Handler<NetTestMsg.Ping> handleNetPing = new Handler<NetTestMsg.Ping>() {

        @Override
        public void handle(NetTestMsg.Ping event) {
            log.debug("{} received net ping from {}", self, event.getVodSource());
            trigger(new NetTestMsg.Pong(self, event.getVodSource()), network);
        }
    };
    
    private Handler<NetTestMsg.Pong> handleNetPong = new Handler<NetTestMsg.Pong>() {

        @Override
        public void handle(NetTestMsg.Pong event) {
            log.debug("{} received net pong from {}", self, event.getVodSource());
        }
    };
    
    public static class SimplePeerInit extends Init<SimplePeer> {
        public final VodAddress self;
        public final VodAddress partner;
        
        public SimplePeerInit(VodAddress self, VodAddress partner) {
            this.self = self;
            this.partner = partner;
        }
    }
}
