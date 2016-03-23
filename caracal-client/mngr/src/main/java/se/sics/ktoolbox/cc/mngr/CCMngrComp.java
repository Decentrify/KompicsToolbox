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
package se.sics.ktoolbox.cc.mngr;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.global.SchemaData;
import se.sics.kompics.Channel;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.bootstrap.CCOperationPort;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapReady;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.event.status.CCHeartbeatReady;
import se.sics.ktoolbox.cc.mngr.event.CCMngrStatus;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(CCMngrComp.class);
    private String logPrefix = " ";

    //********************************CONNECTIONS*******************************
    //***************************EXTERNAL_CONNECT_TO****************************
    private Negative<CCHeartbeatPort> heartbeatPort = provides(CCHeartbeatPort.class);
    private Negative<CCOperationPort> operationPort = provides(CCOperationPort.class);
    private Negative<StatusPort> myStatusPort = provides(StatusPort.class);
    //***************************INTERNAL_NO_CONNECT****************************
    private Positive<StatusPort> otherStatusPort = requires(StatusPort.class);
    //********************************CONFIGURATION*****************************
    //********************************EXTERNAL_STATE****************************
    private final KAddress selfAdr;
    private final ExtPort extPorts;
    //********************************AUX)_STATE********************************
    private SchemaData schemas;
    private Pair<Boolean, Boolean> ready = Pair.with(false, false);
    //*********************************CLEANUP**********************************
    private Pair<Component, Channel[]> caracalClient;
    private Pair<Component, Channel[]> ccHeartbeat;

    public CCMngrComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";

        extPorts = init.extPorts;

        subscribe(handleStart, control);
        subscribe(handleCaracalReady, otherStatusPort);
        subscribe(handleHeartbeatReady, otherStatusPort);

        connectCaracalClient();
        connectCCHeartbeat();
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    ClassMatchedHandler handleCaracalReady
            = new ClassMatchedHandler<CCBootstrapReady, Status.Internal<CCBootstrapReady>>() {

                @Override
                public void handle(CCBootstrapReady content, Status.Internal<CCBootstrapReady> container) {
                    LOG.info("{}caracal ready", logPrefix);
                    schemas = content.caracalSchemaData;
                    ready = ready.with(true, ready.getValue1());
                    checkIfReady();
                }
            };

    ClassMatchedHandler handleHeartbeatReady
            = new ClassMatchedHandler<CCHeartbeatReady, Status.Internal<CCHeartbeatReady>>() {
                @Override
                public void handle(CCHeartbeatReady content, Status.Internal<CCHeartbeatReady> container) {
                    LOG.info("{}heartbeating ready", logPrefix);
                    ready = ready.with(ready.getValue0(), true);
                    checkIfReady();
                }
            };

    private void checkIfReady() {
        if (ready.getValue0() && ready.getValue1()) {
            LOG.info("{}ready", logPrefix);
            trigger(new Status.Internal(new CCMngrStatus.Ready(schemas)), myStatusPort);
        }
    }

    ClassMatchedHandler handleCaracalDisconnect
            = new ClassMatchedHandler<CCBootstrapDisconnected, Status.Internal<CCBootstrapDisconnected>>() {

                @Override
                public void handle(CCBootstrapDisconnected content, Status.Internal<CCBootstrapDisconnected> container) {
                    LOG.warn("{}caracal client disconnected", logPrefix);
                    trigger(new CCMngrStatus.Disconnected(), myStatusPort);
                }
            };

    private void connectCaracalClient() {
        Component ccComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(selfAdr));
        Channel[] ccChannels = new Channel[4];
        ccChannels[0] = connect(ccComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        ccChannels[1] = connect(ccComp.getNegative(Network.class), extPorts.networkPort, Channel.TWO_WAY);
        ccChannels[2] = connect(ccComp.getPositive(StatusPort.class), otherStatusPort.getPair(), Channel.TWO_WAY);
        ccChannels[3] = connect(ccComp.getPositive(CCOperationPort.class), operationPort, Channel.TWO_WAY);
        caracalClient = Pair.with(ccComp, ccChannels);
    }

    private void connectCCHeartbeat() {
        Component ccHComp = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(selfAdr));
        Channel[] ccHChannels = new Channel[5];
        ccHChannels[0] = connect(ccHComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        ccHChannels[1] = connect(ccHComp.getNegative(CCOperationPort.class), caracalClient.getValue0().getPositive(CCOperationPort.class), Channel.TWO_WAY);
        ccHChannels[2] = connect(ccHComp.getNegative(StatusPort.class), caracalClient.getValue0().getPositive(StatusPort.class), Channel.TWO_WAY);
        ccHChannels[3] = connect(ccHComp.getPositive(StatusPort.class), otherStatusPort.getPair(), Channel.TWO_WAY);
        ccHChannels[4] = connect(ccHComp.getPositive(CCHeartbeatPort.class), heartbeatPort, Channel.TWO_WAY);
        ccHeartbeat = Pair.with(ccHComp, ccHChannels);
    }

    public static class Init extends se.sics.kompics.Init<CCMngrComp> {

        public final KAddress selfAdr;
        public final ExtPort extPorts;

        public Init(KAddress selfAdr, ExtPort extPorts) {
            this.selfAdr = selfAdr;
            this.extPorts = extPorts;
        }
    }

    public static class ExtPort {

        public final Positive<Timer> timerPort;
        public final Positive<Network> networkPort;

        public ExtPort(Positive<Timer> timer, Positive<Network> networkPort) {
            this.timerPort = timer;
            this.networkPort = networkPort;
        }
    }
}
