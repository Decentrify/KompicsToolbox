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
package se.sics.p2ptoolbox.caracalclient.bootstrap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Address;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CaracalClientConfig {

    private final static Logger log = LoggerFactory.getLogger(CaracalClientConfig.class);

    public final Address self;
    public final List<Address> caracalNodes;

    public CaracalClientConfig(Config config) {
        this.caracalNodes = new ArrayList<Address>();
        try {
            InetAddress selfIp = InetAddress.getByName(config.getString("system.self.ip"));
            int selfPort = config.getInt("system.self.port");
            self = new Address(selfIp, selfPort, (byte)0);
            List<String> nodeNames = config.getStringList("caracal.nodes");
            for (String nodeName : nodeNames) {
                InetAddress nodeIp = InetAddress.getByName(config.getString("caracal." + nodeName + ".ip"));
                int nodePort = config.getInt("caracal." + nodeName + ".port");
                caracalNodes.add(new Address(nodeIp, nodePort, (byte) 0));
            }
        } catch (ConfigException.Missing ex) {
            log.error("bad configuration - missing parameter:{}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (UnknownHostException ex) {
            log.error("bad configuration - bad ip:{}", ex.getMessage());
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            log.error("configuration error:", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
