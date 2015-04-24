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

package se.sics.p2ptoolbox.caracalclient.heartbeat;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCHeartbeatConfig {
    private static final Logger LOG = LoggerFactory.getLogger(CCHeartbeatComp.class);
    public final long heartbeatPeriod;
    public final String schemaName;
    public final int heartbeatSize;
    
    public CCHeartbeatConfig(long heartbeatPeriod, String schemaName, int heartbeatSize) {
        this.heartbeatPeriod = heartbeatPeriod;
        this.schemaName = schemaName;
        this.heartbeatSize = heartbeatSize;
    }
    
    public CCHeartbeatConfig(Config config) {
        try{
            this.heartbeatPeriod = config.getLong("caracal-client.heartbeat.period");
            this.schemaName = config.getString("caracal-client.heartbeat.schemaName");
            this.heartbeatSize = config.getInt("caracal-client.heartbeat.schemaName");
        } catch(ConfigException.Missing ex) {
            LOG.error("configuration problem:{}", ex.getMessage());
            throw new RuntimeException("CCHeartbeat configuration problem", ex);
        }
    }
}
