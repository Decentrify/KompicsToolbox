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
package se.sics.ktoolbox.overlaymngr;

import se.sics.ktoolbox.overlaymngr.core.OMngrHostComp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.MessageRegistrator;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.gradient.GradientSerializerSetup;
import se.sics.ktoolbox.netmngr.NetworkMngrComp;
import se.sics.ktoolbox.netmngr.NetworkMngrSerializerSetup;
import se.sics.ktoolbox.overlaymngr.core.network.TestSerializerSetup;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrLauncher extends ComponentDefinition {
    
    private static final Logger LOG = LoggerFactory.getLogger(OMngrLauncher.class);
    private String logPrefix = " ";
    
    private Component timerComp;
    private Component networkComp;
    private Component hostComp;
    
    public OMngrLauncher() {
        LOG.info("initiating...");
        
        timerComp = create(JavaTimer.class, Init.NONE);
        NetworkMngrComp.ExtPort netExtPorts = new NetworkMngrComp.ExtPort(timerComp.getPositive(Timer.class));
        networkComp = create(NetworkMngrComp.class, new NetworkMngrComp.Init(netExtPorts));
        OMngrHostComp.ExtPort omngrExtPorts = new OMngrHostComp.ExtPort(timerComp.getPositive(Timer.class),
                networkComp.getPositive(Network.class), networkComp.getPositive(AddressUpdatePort.class)); 
        hostComp = create(OMngrHostComp.class, new OMngrHostComp.Init(omngrExtPorts));
        connect(hostComp.getNegative(AddressUpdatePort.class), networkComp.getPositive(AddressUpdatePort.class), Channel.TWO_WAY);
    }
    
    private static void systemSetup() {
        //serializers setup
        int serializerId = 128;
        MessageRegistrator.register();
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = CroupierSerializerSetup.registerSerializers(serializerId);
        serializerId = GradientSerializerSetup.registerSerializers(serializerId);
        serializerId = OMngrSerializerSetup.registerSerializers(serializerId);
        serializerId = NetworkMngrSerializerSetup.registerSerializers(serializerId);
        serializerId = TestSerializerSetup.registerSerializers(serializerId);
        
        if (serializerId > 255) {
            throw new RuntimeException("switch to bigger serializerIds, last serializerId:" + serializerId);
        }

        //hooks setup
        //no hooks needed
    }
    
    public static void main(String[] args) {
        systemSetup();
        start();
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
    
    public static void start() {
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(OMngrLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
    }
    
    public static void stop() {
        Kompics.shutdown();
    }
}
