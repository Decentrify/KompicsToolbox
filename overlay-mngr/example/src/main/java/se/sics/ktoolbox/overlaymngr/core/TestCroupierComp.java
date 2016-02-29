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
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.GradientSample;
import se.sics.ktoolbox.overlaymngr.util.ServiceView;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.update.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestCroupierComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(TestCroupierComp.class);
    private String logPrefix = " ";

    private final Positive croupierPort = requires(CroupierPort.class);
    private final Negative viewUpdatePort = provides(ViewUpdatePort.class);
    

    public TestCroupierComp(TestCroupierInit init) {
        
        subscribe(handleStart, control);
        subscribe(handleCroupierSample, croupierPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            trigger(new OverlayViewUpdate.Indication(false, new ServiceView()), viewUpdatePort);
        }
    };

    Handler handleCroupierSample = new Handler<CroupierSample>() {
        @Override
        public void handle(CroupierSample event) {
            LOG.info("{}- {}public sample:{}", new Object[]{logPrefix, event.id, event.publicSample.keySet()});
            LOG.info("{}- {}private sample:{}", new Object[]{logPrefix, event.id, event.privateSample.keySet()});
        }
    };
    
    
    public static class TestCroupierInit extends Init<TestCroupierComp> {
        public TestCroupierInit() {
        }
    }
}
