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
package se.sics.p2ptoolbox.util.network.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SimNetworkComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(SimNetworkComp.class);
    private String logPrefix = "";
    
    private final DecoratedAddress privateAdr;
    private final Positive<Network> remote = requires(Network.class);
    private final Negative<Network> local = provides(Network.class);

    public SimNetworkComp(SimNetworkInit init) {
        privateAdr = init.privateAdr;
        logPrefix = "<nid:" + init.privateAdr.getId() + "> ";
        LOG.info("{}initiating...", logPrefix);
        subscribe(handleStart, control);
        subscribe(handleLocal, local);
        subscribe(handleRemote, remote);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };
    
    Handler handleLocal = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Object> contentMsg
                        = (BasicContentMsg) msg;
                LOG.trace("{}received outgoing:{} from:{}({}) to:{}", 
                        new Object[]{logPrefix, contentMsg.getContent(), contentMsg.getSource(), privateAdr, contentMsg.getDestination()});
            contentMsg.getHeader().changeBasicHeader(new BasicHeader(privateAdr, contentMsg.getDestination(), contentMsg.getProtocol()));
            trigger(contentMsg, remote);
        }
    };
    
    Handler handleRemote = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Object> contentMsg
                        = (BasicContentMsg) msg;
                LOG.trace("{}received incoming:{} from:{} to:{}", 
                        new Object[]{logPrefix, contentMsg.getContent(), contentMsg.getSource(), contentMsg.getDestination()});
            trigger(contentMsg, local);
        }
    };

    public static class SimNetworkInit extends Init<SimNetworkComp> {
        public DecoratedAddress privateAdr;
        
        public SimNetworkInit(DecoratedAddress privateAdr) {
            this.privateAdr = privateAdr;
        }
    }
}

