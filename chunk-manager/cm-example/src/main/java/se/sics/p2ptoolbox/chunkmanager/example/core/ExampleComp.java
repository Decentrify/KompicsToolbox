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
package se.sics.p2ptoolbox.chunkmanager.example.core;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ExampleComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ExampleComp.class);

    private Positive network = requires(Network.class);

    private final String logPrefix;

    private final Address selfAddress;
    private final Address partner;
    private final Random rand;

    public ExampleComp(ExampleInit init) {
        this.selfAddress = init.selfAddress;
        this.logPrefix = selfAddress.toString();
        this.rand = init.rand;
        this.partner = init.partner;

        log.info("{} intiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleNetMsg, network);
    }

    Handler handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
            if (partner != null) {
                Header header;
                Object content;

                byte[] contentB = new byte[500];
                rand.nextBytes(contentB);
                
                log.debug("{} sending small TCP message", logPrefix);
                header = new DecoratedHeader(selfAddress, partner, Transport.TCP);
                content = new ExampleMsg(contentB);
                trigger(new BasicContentMsg(header, content), network);
                
                log.debug("{} sending small UDP message", logPrefix);
                header = new DecoratedHeader(selfAddress, partner, Transport.UDP);
                content = new ExampleMsg(contentB);
                trigger(new BasicContentMsg(header, content), network);
                
                contentB = new byte[10000];
                rand.nextBytes(contentB);
                
                log.debug("{} sending big TCP message", logPrefix);
                header = new DecoratedHeader(selfAddress, partner, Transport.TCP);
                content = new ExampleMsg(contentB);
                trigger(new BasicContentMsg(header, content), network);
                
                log.debug("{} sending big UDP message", logPrefix);
                header = new DecoratedHeader(selfAddress, partner, Transport.UDP);
                content = new ExampleMsg(contentB);
                trigger(new BasicContentMsg(header, content), network);
            }

        }
    };
    
    Handler handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };

    Handler<BasicContentMsg> handleNetMsg = new Handler<BasicContentMsg>() {

        @Override
        public void handle(BasicContentMsg msg) {
            log.info("{} received msg with content:{}", logPrefix, msg.getContent());
        }
    };

    public static class ExampleInit extends Init<ExampleComp> {

        public final Address selfAddress;
        public final Address partner;
        public final Random rand;

        public ExampleInit(Address selfAddress, Address partner, long seed) {
            this.selfAddress = selfAddress;
            this.partner = partner;
            this.rand = new Random(seed + 1);
        }
    }
}
