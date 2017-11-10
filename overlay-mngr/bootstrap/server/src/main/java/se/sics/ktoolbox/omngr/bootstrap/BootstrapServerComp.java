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
package se.sics.ktoolbox.omngr.bootstrap;

import com.google.common.collect.HashBasedTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.omngr.bootstrap.msg.Heartbeat;
import se.sics.ktoolbox.omngr.bootstrap.msg.Sample;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapServerComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapServerComp.class);
    private String logPrefix = "";

    //******************************CONNECTIONS*********************************
    Positive<Network> networkPort = requires(Network.class);
    Positive<Timer> timerPort = requires(Timer.class);
    //*****************************EXTERNAL_STATE*******************************
    private KAddress selfAdr;
    //*****************************INTERNAL_STATE*******************************
    private HashBasedTable<Identifier, Integer, KAddress> samples = HashBasedTable.create();

    public BootstrapServerComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleHeartbeat, networkPort);
        subscribe(handleSampleRequest, networkPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    ClassMatchedHandler handleHeartbeat
            = new ClassMatchedHandler<Heartbeat, BasicContentMsg<?, ?, Heartbeat>>() {

                @Override
                public void handle(Heartbeat content, BasicContentMsg<?, ?, Heartbeat> container) {
                    LOG.trace("{}received:{}", logPrefix, container);
                    samples.put(content.overlayId, content.position, container.getHeader().getSource());
                }
            };

    ClassMatchedHandler handleSampleRequest
            = new ClassMatchedHandler<Sample.Request, BasicContentMsg<?, ?, Sample.Request>>() {

                @Override
                public void handle(Sample.Request content, BasicContentMsg<?, ?, Sample.Request> container) {
                    LOG.trace("{}received:{}", logPrefix, container);
                    List<KAddress> sample = sampleWithoutDuplicates(content.overlayId);
                    KContentMsg response = container.answer(content.answer(sample));
                    LOG.trace("{}sending:{}", logPrefix, response);
                    trigger(response, networkPort);
                }
            };

    private List<KAddress> sampleWithoutDuplicates(Identifier overlayId) {
        Set<Identifier> ids = new HashSet<>();
        List<KAddress> adrs = new ArrayList<>();
        for (KAddress adr : samples.row(overlayId).values()) {
            if (ids.contains(adr.getId())) {
                continue;
            }
            adrs.add(adr);
            ids.add(adr.getId());
        }
        return adrs;
    }

    public static class Init extends se.sics.kompics.Init<BootstrapServerComp> {

        public final KAddress selfAdr;

        public Init(KAddress selfAdr) {
            this.selfAdr = selfAdr;
        }
    }
}
