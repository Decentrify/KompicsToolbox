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

package se.sics.ktoolbox.util.proxy.example.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.ktoolbox.util.proxy.example.core.ExampleEvent;
import se.sics.ktoolbox.util.proxy.example.core.PortX;
import se.sics.ktoolbox.util.proxy.example.core.PortY;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ComponentB extends ComponentDefinition {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentB.class);
    private String logPrefix = "";

    private final Negative portX = provides(PortX.class);
    private final Positive portY = requires(PortY.class);
    
    public ComponentB(InitB init) {
        LOG.info("{}initiating...", logPrefix);
        subscribe(handleStart, control);
        subscribe(handleY, portY);
        subscribe(handleX, portX);
    }

    //***************************CONTROL****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    //**************************************************************************
    Handler handleY = new Handler<ExampleEvent.Y>() {
        @Override
        public void handle(ExampleEvent.Y event) {
            LOG.trace("{}received:{}", logPrefix, event);
            trigger(new ExampleEvent.X(), portX);
        }
    };
    
    Handler handleX = new Handler<ExampleEvent.X>() {
        @Override
        public void handle(ExampleEvent.X event) {
            LOG.trace("{}received:{}", logPrefix, event);
            trigger(new ExampleEvent.Y(), portY);
        }
    };

    public static class InitB extends Init<ComponentB> {
    }
}
