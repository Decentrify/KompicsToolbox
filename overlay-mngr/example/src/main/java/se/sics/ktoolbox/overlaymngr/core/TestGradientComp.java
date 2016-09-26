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
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestGradientComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(TestGradientComp.class);
    private String logPrefix = " ";

    private final Positive croupierPort = requires(CroupierPort.class);
    private final Positive gradientPort = requires(GradientPort.class);
    private final Negative viewUpdatePort = provides(OverlayViewUpdatePort.class);
    
    private final int id;
    private final OverlayId tgradientId;
    
    public TestGradientComp(TestGradientInit init) {
        id = init.id;
        tgradientId = init.tgradientId;
        
        subscribe(handleStart, control);
        subscribe(handleCroupierSample, croupierPort);
        subscribe(handleGradientSample, gradientPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            OverlayViewUpdate.Indication ovu = new OverlayViewUpdate.Indication(tgradientId, false, new IdView(id));
            LOG.info("{}sending:{}", new Object[]{logPrefix, ovu});
            trigger(ovu, viewUpdatePort);
        }
    };

    Handler handleCroupierSample = new Handler<CroupierSample>() {
        @Override
        public void handle(CroupierSample sample) {
            LOG.info("{}{}", new Object[]{logPrefix, sample});
            LOG.info("{}public sample size:{}, private sample size:{}", 
                    new Object[]{logPrefix, sample.publicSample.size(), sample.privateSample.size()});
        }
    };
    
    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
            LOG.info("{}{}", new Object[]{logPrefix, sample});
            LOG.info("{}neighbours size:{} fingers size:{}", 
                    new Object[]{logPrefix, sample.getGradientNeighbours().size(), sample.getGradientFingers().size()});
        }
    };
    
    
    public static class TestGradientInit extends Init<TestGradientComp> {
        public final int id;
        public final OverlayId tgradientId;
        
        public TestGradientInit(int id, OverlayId tgradientId) {
            this.id = id;
            this.tgradientId = tgradientId;
        }
    }
}
