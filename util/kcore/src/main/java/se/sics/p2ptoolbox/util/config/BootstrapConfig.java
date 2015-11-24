///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.p2ptoolbox.util.config;
//
//import com.typesafe.config.Config;
//import com.typesafe.config.ConfigException;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.ArrayList;
//import java.util.List;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.ktoolbox.util.address.impl.BasicAddress;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class BootstrapConfig {
//    private final static Logger LOG = LoggerFactory.getLogger("KompicsConfig");
//
//    public final List<DecoratedAddress>bootstrapNodes;
//    
//    public BootstrapConfig(Config config) {
//        this.bootstrapNodes = new ArrayList<DecoratedAddress>();
//        List<String> boostrapNodeNames;
//
//        try {
//            boostrapNodeNames = config.getStringList("system.bootstrap.nodes");
//        } catch (ConfigException.Missing ex) {
//            LOG.info("no bootstrap nodes");
//            return;
//        }
//
//        for (String bootstrapNodeName : boostrapNodeNames) {
//            try {
//                InetAddress bootstrapIp = InetAddress.getByName(config.getString("system.bootstrap." + bootstrapNodeName + ".ip"));
//                int bootstrapPort = config.getInt("system.bootstrap." + bootstrapNodeName + ".port");
//                int bootstrapId = config.getInt("system.bootstrap." + bootstrapNodeName + ".id");
//                bootstrapNodes.add(new DecoratedAddress(new BasicAddress(bootstrapIp, bootstrapPort, bootstrapId)));
//            } catch (UnknownHostException ex) {
//                LOG.error("bad bootstrap address");
//                throw new RuntimeException("bad system config - bootstrap address", ex);
//            } catch (ConfigException.Missing ex) {
//                LOG.error("bad bootstrap address");
//                throw new RuntimeException("bad system config - bootstrap address", ex);
//            }
//            LOG.info("bootstrap nodes:{}", bootstrapNodes);
//        }
//    }
//}
