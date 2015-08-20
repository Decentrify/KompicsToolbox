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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.chunkmanager.ChunkManagerComp;
import se.sics.p2ptoolbox.chunkmanager.ChunkManagerConfig;
import se.sics.p2ptoolbox.util.config.SystemConfig;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ExampleHostComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ExampleHostComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final SystemConfig systemConfig;
    private final ChunkManagerConfig cmConfig;
    private final Address partner;

    private final String logPrefix;

    public ExampleHostComp(HostInit init) {
        this.systemConfig = init.systemConfig;
        this.cmConfig = init.cmConfig;
        this.partner = init.partner;
        this.logPrefix = systemConfig.self.toString();
        log.info("{} initiating...", logPrefix);
        log.debug("{}  partner:{}", new Object[]{logPrefix, partner});

        Component chunkManager = createNConnectChunkManager();
        Component exampleComp = createNConnectExampleComp(chunkManager);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
    }
    
    private Component createNConnectChunkManager() {
        Component chunkManager = create(ChunkManagerComp.class, new ChunkManagerComp.CMInit(systemConfig, cmConfig));
        connect(chunkManager.getNegative(Network.class), network);
        connect(chunkManager.getNegative(Timer.class), timer);
        return chunkManager;
    }

    private Component createNConnectExampleComp(Component chunkManager) {
        Component example = create(ExampleComp.class, new ExampleComp.ExampleInit(systemConfig.self, partner, systemConfig.seed));
        connect(example.getNegative(Network.class), chunkManager.getPositive(Network.class));
        return example;
    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
        }
    };

    private Handler<Stop> handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };

    public static class HostInit extends Init<ExampleHostComp> {

        public final SystemConfig systemConfig;
        public final ChunkManagerConfig cmConfig;
        public final Address partner;

        public HostInit(String configFile, Address partner) {
            Config config = ConfigFactory.load(configFile);
            this.systemConfig = new SystemConfig(config);
            this.cmConfig = new ChunkManagerConfig(config);
            this.partner = partner;
        }

        public HostInit(SystemConfig systemConfig, ChunkManagerConfig cmConfig, Address partner) {
            this.systemConfig = systemConfig;
            this.cmConfig = cmConfig;
            this.partner = partner;
        }
    }
}