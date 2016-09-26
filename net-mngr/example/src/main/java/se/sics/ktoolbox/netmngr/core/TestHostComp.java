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
package se.sics.ktoolbox.netmngr.core;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayIdFactory;
import se.sics.ktoolbox.util.idextractor.MsgOverlayIdExtractor;
import se.sics.ktoolbox.util.network.ports.One2NChannel;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestHostComp extends ComponentDefinition {
    private static final Logger LOG = LoggerFactory.getLogger(TestHostComp.class);
    private String logPrefix = " ";

    //***************************CONNECTIONS************************************
    //provided external connections - do NOT connect to
    private final ExtPort extPorts;
    private One2NChannel<Network> networkEnd;
    //****************************CONFIGURATION*********************************
    private TestHostKCWrapper hostConfig;
    //*******************************CLEANUP************************************
    private Pair<Component, Channel[]> test1, test2;

    public TestHostComp(Init init) {
        LOG.info("{}initiating...", logPrefix);
        hostConfig = new TestHostKCWrapper(config());
        
        extPorts = init.extPorts;
        networkEnd = One2NChannel.getChannel("host", extPorts.networkPort, new MsgOverlayIdExtractor());
        
        subscribe(handleStart, control);
        connectTest1Comp();
        connectTest2Comp();
    }
    
    //*****************************CONTROL**************************************
    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };
    //******************************CONNECTIONS*********************************
    private void connectTest1Comp() {
        byte ownerId = 1;
        IntIdFactory intIdFactory = new IntIdFactory(null);
        OverlayIdFactory overlayIdFactory = new OverlayIdFactory(intIdFactory, OverlayId.BasicTypes.OTHER, ownerId);
        OverlayId overlayId = overlayIdFactory.id(new BasicBuilders.IntBuilder(1));
        Component test1Comp = create(TestComp.class, new TestComp.Init(hostConfig.partner, overlayId));
        Channel[] test1Channel = new Channel[1];
        test1Channel[0] = connect(test1Comp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        networkEnd.addChannel(overlayId, test1Comp.getNegative(Network.class));
        test1 = Pair.with(test1Comp, test1Channel);
    }
    
    private void connectTest2Comp() {
        byte ownerId = 1;
        IntIdFactory intIdFactory = new IntIdFactory(null);
        OverlayIdFactory overlayIdFactory = new OverlayIdFactory(intIdFactory, OverlayId.BasicTypes.OTHER, ownerId);
        OverlayId overlayId = overlayIdFactory.id(new BasicBuilders.IntBuilder(2));
        Component test2Comp = create(TestComp.class, new TestComp.Init(hostConfig.partner, overlayId));
        Channel[] test2Channel = new Channel[1];
        test2Channel[0] = connect(test2Comp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        networkEnd.addChannel(overlayId, test2Comp.getNegative(Network.class));
        test2 = Pair.with(test2Comp, test2Channel);
    }
    //**************************************************************************
    public static class Init extends se.sics.kompics.Init<TestHostComp> {
        public final ExtPort extPorts;
        
        public Init(ExtPort extPorts) {
            this.extPorts = extPorts;
        }
    }
    
    public static class ExtPort {
        public final Positive<Network> networkPort;
        public final Positive<Timer> timerPort;
        
        public ExtPort(Positive<Network> networkPort, Positive<Timer> timerPort) {
            this.networkPort = networkPort;
            this.timerPort = timerPort;
        }
    }
}
