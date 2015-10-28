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
package se.sics.ktoolbox.overlaymngr.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp;
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp.OverlayMngrInit;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrHostComp extends ComponentDefinition {
    private static final Logger LOG = LoggerFactory.getLogger(OMngrHostComp.class);
    private String logPrefix = " ";
    
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<Network> network = requires(Network.class);
    
    private final OMngrHostKCWrapper config;
    private DecoratedAddress self;
    
    private Component overlayMngr;
    
    public OMngrHostComp(OMngrHostInit init) {
        this.config = init.config;
        this.self = config.self;
        this.logPrefix = self.getBase().toString() + " ";
        LOG.info("{}initiating...", logPrefix);
        
        subscribe(handleStart, control);
        connectOverlayMngr();
    }
    
    private void connectOverlayMngr() {
        overlayMngr = create(OverlayMngrComp.class, new OverlayMngrInit(config.getKConfigCore(), self));
        connect(overlayMngr.getNegative(Timer.class), timer);
        connect(overlayMngr.getNegative(Network.class), network);
//        connect(overlayMngr.getPositive(SelfAddressUpdatePort.class), ...);
    }
    
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };
    
    public static class OMngrHostInit extends Init<OMngrHostComp> {
        public final OMngrHostKCWrapper config;
        
        public OMngrHostInit(OMngrHostKCWrapper config) {
            this.config = config;
        }
    }
}