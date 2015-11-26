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
package se.sics.ktoolbox.aggregator.server;

import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.ktoolbox.aggregator.util.AggregatorPacket;
import se.sics.ktoolbox.aggregator.server.event.SystemWindow;
import se.sics.ktoolbox.aggregator.server.event.VisualizerWindow;
import java.util.*;
import org.javatuples.Pair;
import se.sics.kompics.network.Address;
import se.sics.ktoolbox.aggregator.server.util.VisualizerPacket;
import se.sics.ktoolbox.aggregator.server.util.VisualizerProcessor;
import se.sics.ktoolbox.aggregator.util.AggregatorProcessor;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.impl.SystemKCWrapper;

/**
 * Visualizer component used for providing the visualizations to the end user.
 *
 * Created by babbar on 2015-09-02.
 */
public class Visualizer extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(Visualizer.class);
    private String logPrefix;

    Negative visualizerPort = provides(VisualizerPort.class);
    Positive aggregatorPort = requires(GlobalAggregatorPort.class);

    private final SystemKCWrapper systemConfig;
    private final VisualizerKCWrapper visualizerConfig;

    private final Map<Class, VisualizerProcessor> visualizerProcessors;
    private final Map<Integer, Table<Address, Class, AggregatorPacket>> snapshots = new HashMap<>();
    private Pair<Integer, Integer> snapshotInterval;

    public Visualizer(VisualizerInit init) {
        systemConfig = new SystemKCWrapper(init.configCore);
        visualizerConfig = new VisualizerKCWrapper(init.configCore);
        logPrefix = "<nid:" + systemConfig.id + ">";
        LOG.info("{}initiating...", logPrefix);

        visualizerProcessors = init.visualizerProcessors;
        snapshotInterval = Pair.with(0, 0);

        subscribe(handleStart, control);
        subscribe(handleSystemWindow, aggregatorPort);
        subscribe(handleVisualizationRequest, visualizerPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    /**
     * Handler invoked when the visualizer receives the aggregated information
     * from the global aggregator.
     */
    Handler handleSystemWindow = new Handler<SystemWindow>() {
        @Override
        public void handle(SystemWindow event) {
            LOG.trace("{}received:{}", logPrefix, event);

            while (snapshotInterval.getValue1() - snapshotInterval.getValue0() >= visualizerConfig.snapshotMaxSize) {
                snapshots.remove(snapshotInterval.getValue0());
                snapshotInterval = snapshotInterval.setAt0(snapshotInterval.getValue0() + 1);
            }

            if (event.systemWindow.isEmpty()) {
                LOG.debug("{}empty window", logPrefix);
                return;
            }
            snapshotInterval = snapshotInterval.setAt1(snapshotInterval.getValue1() + 1);
            snapshots.put(snapshotInterval.getValue1(), event.systemWindow);
        }
    };

    Handler handleVisualizationRequest = new Handler<VisualizerWindow.Request>() {
        @Override
        public void handle(VisualizerWindow.Request request) {
            LOG.trace("{}received:{}", logPrefix, request);

            VisualizerProcessor processor = visualizerProcessors.get(request.processor);

            if (processor == null) {
                LOG.error("{}unable to locate designer:{}", logPrefix, request.processor);
                throw new RuntimeException("unable to locate designer:" + request.processor);
            }

            LOG.debug("{}processor:{} window start:{} end:{}",
                    new Object[]{logPrefix, request.processor, request.startLoc, request.endLoc});
            VisualizerPacket window = processor.getFirst();
            int startLoc;
            int endLoc;
            if (request.endLoc < snapshotInterval.getValue0()
                    || request.startLoc > snapshotInterval.getValue1()) {
                LOG.info("{}no aggregated windows within the requested visualizer window");
                answer(request, request.answer(window));
            } else {
                startLoc = (request.startLoc < snapshotInterval.getValue0() ? snapshotInterval.getValue0() : request.startLoc);
                endLoc = (request.endLoc > snapshotInterval.getValue1() ? snapshotInterval.getValue1() : request.endLoc);

                for (int i = startLoc; i <= endLoc; i++) {
                    window = processor.process(window, snapshots.get(i));
                }
                
                LOG.debug("{}answering processor:{} window start:{} end:{}",
                    new Object[]{logPrefix, request.processor, startLoc, endLoc});
                answer(request, request.answer(window));
            }
        }
    };

    public static class VisualizerInit extends Init<Visualizer> {

        public final KConfigCore configCore;
        public final Map<Class, VisualizerProcessor> visualizerProcessors;

        public VisualizerInit(KConfigCore configCore, Map<Class, VisualizerProcessor> visualizerProcessors) {
            this.configCore = configCore;
            this.visualizerProcessors = visualizerProcessors;
        }
    }
}
