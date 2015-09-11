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

package se.sics.p2ptoolbox.util.proxy.example.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.p2ptoolbox.util.proxy.example.core.HPMsg;
import se.sics.p2ptoolbox.util.proxy.example.core.PortX;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ComponentA extends ComponentDefinition {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentA.class);
    private String logPrefix = "";
    
    private final Negative portX = provides(PortX.class);
    private final Positive portZ = requires(PortZ.class);
    
    private final boolean fail;
    
    public ComponentA(InitA init) {
        this.fail = init.fail;
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleHPMsg, portZ);
    }
    
    //**************************************************************************
    Handler handleHPMsg = new Handler<HPMsg.X>() {
        @Override
        public void handle(HPMsg.X msg) {
            LOG.info("hp msg");
            if(fail) {
                throw new RuntimeException("la");
            }
            trigger(msg, portX);
        }
    };

    //***************************CONTROL****************************************
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

    public static class InitA extends Init<ComponentA> {
        public final boolean fail;
        
        public InitA(boolean fail) {
            this.fail = fail;
        }
    }
}
