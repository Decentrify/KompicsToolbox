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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SystemConfig {

    private final static Logger LOG = LoggerFactory.getLogger(SystemConfig.class);

    private Config config;
    private InetAddress ip;
    
    public long seed;
    public DecoratedAddress self;
    public DecoratedAddress aggregator;
    public List<DecoratedAddress> bootstrapNodes = new ArrayList<DecoratedAddress>();
    private Random rand;
    

    public SystemConfig(Config config) {
        this(config, null);
    }
    
    public SystemConfig(Config config, InetAddress ip) {
        this.config = config;
        this.ip = ip;
        readConfig();
    }
    
    private void readConfig() {
        try {
            try {
                seed = config.getLong("system.seed");
            } catch (ConfigException.Missing ex) {
                Random r = new SecureRandom();
                seed = r.nextLong();
            }
            rand = new Random(seed);
            
            InetAddress configIp = null;
            try {
                 configIp = InetAddress.getByName(config.getString("system.self.ip"));
            } catch (ConfigException.Missing ex) {
                if(ip == null) {
                    throw ex;
                }
            }
            if(ip == null) {
                ip = configIp;
            } else if(!ip.equals(configIp)) {
                LOG.warn("Direct providedIp:{} is different than configIp:{}. Proceeding with providedIp:{}", 
                        new Object[]{ip, configIp, ip});
            }
            
            int selfPort = config.getInt("system.self.port");
            int selfId;
            try {
                selfId = config.getInt("system.self.id");
            } catch (ConfigException.Missing ex) {
                selfId = rand.nextInt();
            }
            this.self = new DecoratedAddress(new BasicAddress(ip, selfPort, selfId));
            LOG.info("self address:{}", self);
        } catch (UnknownHostException ex) {
            LOG.error("bad self address");
            throw new RuntimeException("bad system config - self address", ex);
        } catch (ConfigException.Missing ex) {
            LOG.error("bad self address");
            throw new RuntimeException("bad system config - self address", ex);
        }
        try {
            InetAddress aggregatorIp = InetAddress.getByName(config.getString("system.aggregator.ip"));
            int aggregatorPort = config.getInt("system.aggregator.port");
            int aggregatorId = config.getInt("system.aggregator.id");
            this.aggregator = new DecoratedAddress(new BasicAddress(aggregatorIp, aggregatorPort, aggregatorId));
            LOG.info("aggregator address:{}", aggregator);
        } catch (UnknownHostException ex) {
            LOG.error("bad aggregator address");
            throw new RuntimeException("bad system config - aggregator address", ex);
        } catch (ConfigException.Missing ex) {
            LOG.info("no aggregator address");
            this.aggregator = null;
        }

        List<String> boostrapNodeNames;
        try {
            boostrapNodeNames = config.getStringList("system.bootstrap.nodes");
        } catch (ConfigException.Missing ex) {
            LOG.info("no bootstrap nodes");
            return;
        }

        for (String bootstrapNodeName : boostrapNodeNames) {
            try {
                InetAddress bootstrapIp = InetAddress.getByName(config.getString("system.bootstrap." + bootstrapNodeName + ".ip"));
                int bootstrapPort = config.getInt("system.bootstrap." + bootstrapNodeName + ".port");
                int bootstrapId = config.getInt("system.bootstrap." + bootstrapNodeName + ".id");
                bootstrapNodes.add(new DecoratedAddress(new BasicAddress(bootstrapIp, bootstrapPort, bootstrapId)));
            } catch (UnknownHostException ex) {
                LOG.error("bad bootstrap address");
                throw new RuntimeException("bad system config - bootstrap address", ex);
            } catch (ConfigException.Missing ex) {
                LOG.error("bad bootstrap address");
                throw new RuntimeException("bad system config - bootstrap address", ex);
            }
            LOG.info("bootstrap nodes:{}", bootstrapNodes);
        }
    }

    public SystemConfig(long seed, DecoratedAddress self, DecoratedAddress aggregator, List<DecoratedAddress> bootstrapNodes) {
        this.seed = seed;
        this.self = self;
        this.aggregator = aggregator;
        this.bootstrapNodes = bootstrapNodes;
    }
}
