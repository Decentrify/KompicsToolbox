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
package se.sics.ktoolbox.util.address.nat.example;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Start;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.ktoolbox.util.address.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.address.nat.example.AddressResolutionComp.AddressResolutionInit;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HostComp extends ComponentDefinition {
    private static final Logger LOG = LoggerFactory.getLogger(HostComp.class);
    private String logPrefix = "";
    
    private Component network;
    private Component addressResolution;
    private Component app;
    
    public HostComp(HostInit init) {
        LOG.info("{}initiating...", logPrefix);
        subscribe(handleStart, control);
        
        network = create(NettyNetwork.class, new NettyInit(init.selfAdr));
        addressResolution = create(AddressResolutionComp.class, new AddressResolutionInit(
                init.selfAdr.getPublicAdr().getPort(), 
                (init.targetAdr.isPresent() ? init.targetAdr.))
        ));
    }
    
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            
        }
    };
    
    public static class HostInit extends Init<HostComp> {
        public final NatAwareAddressImpl selfAdr;
        public final Optional<NatAwareAddressImpl> targetAdr;
        public final int appPort;
        
        public HostInit(NatAwareAddressImpl selfAdr, NatAwareAddressImpl targetAdr, int appPort) {
            this.selfAdr = selfAdr;
            this.targetAdr = Optional.fromNullable(targetAdr);
            this.appPort = appPort;
        }
    }
}
