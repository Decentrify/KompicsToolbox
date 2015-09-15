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
package se.sics.p2ptoolbox.util.proxy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DummyNetwork extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(DummyNetwork.class);
    private String logPrefix = "";

    private final Negative localNetwork = provides(Network.class);
    private final Positive network = requires(Network.class);

    public DummyNetwork() {
        LOG.info("{}initiating", logPrefix);
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleLocal, localNetwork);
        subscribe(handleNetwork, network);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{}stopping...", logPrefix);
        }
    };
    
    Handler handleLocal = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            LOG.debug("{}forwarding outgoing:{}", logPrefix, msg);
            trigger(msg, network);
        }
    };
    
    Handler handleNetwork = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            LOG.debug("{}forwarding incoming:{}", logPrefix, msg);
            trigger(msg, localNetwork);
        }
    };
}