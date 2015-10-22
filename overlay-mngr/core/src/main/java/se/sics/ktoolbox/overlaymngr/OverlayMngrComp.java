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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierComp.CroupierInit;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayMngrComp extends ComponentDefinition {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayMngrComp.class);
    private String logPrefix = "";

    private final Positive<Timer> timer = requires(Timer.class);
    private final Positive<SelfAddressUpdatePort> addressUpdate = requires(SelfAddressUpdatePort.class);

    private DecoratedAddress self;
    
    private Component globalCroupier;
    
    public OverlayMngrComp(OverlayMngrInit init) {
        this.self = init.self;
        this.logPrefix = init.self.getBase().toString() + " ";
        LOG.info("{}initiating...", logPrefix);
        
        subscribe(handleStart, control);
        subscribe(handleSelfAddressUpdate, addressUpdate);
        
//        globalCroupier = create(CroupierComp.class, new CroupierInit());
    }

    //****************************CONTROL***************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...");
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.info("{}component:{} fault:{}",
                new Object[]{logPrefix, fault.getSource().getClass(), fault.getCause().getMessage()});
        return Fault.ResolveAction.ESCALATE;
    }
    
    Handler handleSelfAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{}address change from:{} to:{}", new Object[]{logPrefix, self, update.self});
            self = update.self;
        }
    };
    //**************************************************************************
    public static class OverlayMngrInit extends Init<OverlayMngrComp> {

        public final DecoratedAddress self;

        public OverlayMngrInit(DecoratedAddress self) {
            this.self = self;
        }
    }
}
