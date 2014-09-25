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
package se.sics.p2ptoolbox.nettytest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.address.Address;
import se.sics.gvod.net.NatNetworkControl;
import se.sics.gvod.net.NettyInit;
import se.sics.gvod.net.NettyNetwork;
import se.sics.gvod.net.Transport;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.net.events.PortBindRequest;
import se.sics.gvod.net.events.PortBindResponse;
import se.sics.gvod.timer.Timer;
import se.sics.gvod.timer.java.JavaTimer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(HostManagerComp.class);

    private Component network;
    private Component timer;
    private Component host;
    private Address selfAddress = null;
    private Address partnerAddress = null;

    public static class PsPortBindResponse extends PortBindResponse {

        public PsPortBindResponse(PortBindRequest request) {
            super(request);
        }
    }

    public HostManagerComp() {
        log.info("{} create host manager", TestSetup.selfAddress);
        this.selfAddress = TestSetup.selfAddress;
        this.partnerAddress = TestSetup.partnerAddress;
        timer = create(JavaTimer.class, Init.NONE);
        network = create(NettyNetwork.class, new NettyInit(TestSetup.seed, true, TestSetup.msgFrameDecoderClass));
        connect(network.getNegative(Timer.class), timer.getPositive(Timer.class));

        subscribe(startHandler, control);
        subscribe(handlePsPortBindResponse, network.getPositive(NatNetworkControl.class));
    }

    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting host manager - binding port ...", selfAddress);
            PortBindRequest pb1 = new PortBindRequest(selfAddress, TestSetup.transport);
            PsPortBindResponse pbr1 = new PsPortBindResponse(pb1);
            pb1.setResponse(pbr1);

            trigger(pb1, network.getPositive(NatNetworkControl.class));
        }
    };

    private Handler<PsPortBindResponse> handlePsPortBindResponse = new Handler<PsPortBindResponse>() {
        @Override
        public void handle(PsPortBindResponse event) {

            if (event.getStatus() != PortBindResponse.Status.SUCCESS) {
                log.error("Couldn't bind to port {}. Either another instance of the program is"
                        + "already running, or that port is being used by a different program. Go"
                        + "to settings to change the port in use. Status: {}",
                        new Object[]{event.getPort(), event.getStatus()});

                Kompics.shutdown();
                System.exit(-1);
            } else {
                log.info("{} host manager started", selfAddress);
                host = create(HostComp.class, new HostComp.HostInit(new VodAddress(selfAddress, 0), new VodAddress(partnerAddress, 0)));
                
                connect(network.getPositive(VodNetwork.class), host.getNegative(VodNetwork.class));
                connect(timer.getPositive(Timer.class), host.getNegative(Timer.class));
                
                trigger(Start.event, host.getControl());
            }
        }
    };
}
