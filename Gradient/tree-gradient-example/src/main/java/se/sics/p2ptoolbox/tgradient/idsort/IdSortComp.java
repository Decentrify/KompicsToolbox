/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.tgradient.idsort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.p2ptoolbox.gradient.GradientPort;
import se.sics.p2ptoolbox.gradient.msg.GradientSample;
import se.sics.p2ptoolbox.gradient.msg.GradientUpdate;
import se.sics.p2ptoolbox.util.identifiable.IntegerIdentifiable;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class IdSortComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(IdSortComp.class);

    private Positive gradient = requires(GradientPort.class);

    private final Address selfAddress;
    private final String logPrefix;

    public IdSortComp(IdSortInit init) {
        this.selfAddress = init.selfAddress;
        this.logPrefix = "id:" + selfAddress;
        log.info("{} initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleGradientSample, gradient);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
            trigger(new GradientUpdate(new IdView(((IntegerIdentifiable)selfAddress).getId())), gradient);
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };
    
    Handler handleGradientSample = new Handler<GradientSample>() {

        @Override
        public void handle(GradientSample sample) {
            log.info("{} tree-gradient:{}", new Object[]{logPrefix, sample.gradientSample});
//            if(((IntegerIdentifiable)selfAddress).getId() % 2 == 0) {
//                trigger(new GradientUpdate(new IdView(((IntegerIdentifiable)selfAddress).getId() + 1000)), gradient);
//            }
        }
    };

    public static class IdSortInit extends Init<IdSortComp> {

        public final Address selfAddress;

        public IdSortInit(Address selfAddress) {
            this.selfAddress = selfAddress;
        }
    }
}
