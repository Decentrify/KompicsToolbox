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

package se.sics.p2ptoolbox.simulator.example.proj;

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
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MyComponent extends ComponentDefinition {
    
    private static final Logger log = LoggerFactory.getLogger(MyComponent.class);
    
    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Negative<MyPort> myPort = provides(MyPort.class);
    
    private VodAddress self;
    private VodAddress partner;
    
    public MyComponent(MyInit init) {
        log.debug("initiating test node:{}", init.self);
        
        this.self = init.self;
        
        subscribe(handleStart, control);
        subscribe(handleNetPing, network);
        subscribe(handleNetPong, network);
    }
    
    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.debug("starting test node:{}", self);
        }
        
    };
    
    private Handler<MyNetMsg.Ping> handleNetPing = new Handler<MyNetMsg.Ping>() {

        @Override
        public void handle(MyNetMsg.Ping event) {
            log.debug("{} received net ping from {}", self, event.getVodSource());
            trigger(new MyNetMsg.Pong(self, event.getVodSource()), network);
        }
    };
    
    private Handler<MyNetMsg.Pong> handleNetPong = new Handler<MyNetMsg.Pong>() {

        @Override
        public void handle(MyNetMsg.Pong event) {
            log.debug("{} received net pong from {}", self, event.getVodSource());
        }
    };
    
    public static class MyInit extends Init<MyComponent> {
        public final VodAddress self;
        
        public MyInit(VodAddress self) {
            this.self = self;
        }
    }
}
