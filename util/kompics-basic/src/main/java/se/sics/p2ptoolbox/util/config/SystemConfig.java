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
package se.sics.p2ptoolbox.util.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SystemConfig {

    private final static Logger log = LoggerFactory.getLogger(SystemConfig.class);

    public DecoratedAddress self = null;
    public DecoratedAddress aggregator = null;
    public List<DecoratedAddress> bootstrapNodes = new ArrayList<DecoratedAddress>();

    public SystemConfig(Config config) {
        try {
            InetAddress selfIp = InetAddress.getByName(config.getString("system.self.ip"));
            int selfPort = config.getInt("system.self.port");
            int selfId = config.getInt("system.self.id");
            this.self = new DecoratedAddress(new BasicAddress(selfIp, selfPort, selfId));
            log.info("self address:{}", self);
        } catch (UnknownHostException ex) {
            log.error("bad self address");
            throw new RuntimeException("bad system config - self address", ex);
        } catch (ConfigException.Missing ex) {
            log.error("bad self address");
            throw new RuntimeException("bad system config - self address", ex);
        }
        try {
            InetAddress aggregatorIp = InetAddress.getByName(config.getString("system.aggregator.ip"));
            int aggregatorPort = config.getInt("system.aggregator.port");
            int aggregatorId = config.getInt("system.aggregator.id");
            this.aggregator = new DecoratedAddress(new BasicAddress(aggregatorIp, aggregatorPort, aggregatorId));
            log.info("aggregator address:{}", aggregator);
        } catch (UnknownHostException ex) {
            log.error("bad aggregator address");
            throw new RuntimeException("bad system config - aggregator address", ex);
        } catch (ConfigException.Missing ex) {
            log.info("no aggregator address");
        }

        List<String> boostrapNodeNames;
        try {
            boostrapNodeNames = config.getStringList("system.bootstrap.nodes");
        } catch (ConfigException.Missing ex) {
            log.info("no bootstrap nodes");
            return;
        }

        for (String bootstrapNodeName : boostrapNodeNames) {
            try {
                InetAddress bootstrapIp = InetAddress.getByName(config.getString("system.bootstrap." + bootstrapNodeName + ".ip"));
                int bootstrapPort = config.getInt("system.bootstrap." + bootstrapNodeName + ".port");
                int bootstrapId = config.getInt("system.bootstrap." + bootstrapNodeName +  ".id");
                bootstrapNodes.add(new DecoratedAddress(new BasicAddress(bootstrapIp, bootstrapPort, bootstrapId)));
            } catch (UnknownHostException ex) {
                log.error("bad bootstrap address");
                throw new RuntimeException("bad system config - bootstrap address", ex);
            } catch (ConfigException.Missing ex) {
                log.error("bad bootstrap address");
                throw new RuntimeException("bad system config - bootstrap address", ex);
            }
            log.info("bootstrap nodes:{}", bootstrapNodes);
        }
    }
}
