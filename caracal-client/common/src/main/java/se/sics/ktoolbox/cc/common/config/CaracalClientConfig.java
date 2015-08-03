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
package se.sics.ktoolbox.cc.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CaracalClientConfig implements CCBootstrapConfig, CCHeartbeatConfig {

    private static final Logger log = LoggerFactory.getLogger(CaracalClientConfig.class);

    private final long sanityCheckPeriod;
    
    private final long caracalTimeout;
    private final int bootstrapSize;
    
    public final long heartbeatPeriod;
    public final String heartbeatSchemaName;
    public final int heartbeatSize;

    public CaracalClientConfig(Config config) {
        try {
            this.sanityCheckPeriod = config.getLong("system.sanityCheckTimeout");
            
            this.caracalTimeout = config.getLong("caracal-client.bootstrap.caracalTimeout");
            this.bootstrapSize = config.getInt("caracal-client.bootstrap.size");
            
            this.heartbeatPeriod = config.getLong("caracal-client.heartbeat.period");
            this.heartbeatSchemaName = config.getString("caracal-client.heartbeat.schemaName");
            this.heartbeatSize = config.getInt("caracal-client.heartbeat.size");
        } catch (ConfigException.Missing ex) {
            log.error("configuration problem:{}", ex.getMessage());
            throw new RuntimeException("CCBootstrap configuration problem", ex);
        }
    }

    @Override
    public long sanityCheckPeriod() {
        return sanityCheckPeriod;
    }

    @Override
    public long caracalTimeout() {
        return caracalTimeout;
    }

    @Override
    public int bootstrapSize() {
        return bootstrapSize;
    }

    @Override
    public long heartbeatPeriod() {
        return heartbeatPeriod;
    }

    @Override
    public String heartbeatSchemaName() {
        return heartbeatSchemaName;
    }

    @Override
    public int heartbeatSize() {
        return heartbeatSize;
    }
}
