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
package se.sics.ktoolbox.util.config.options;

import com.google.common.base.Optional;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.config.KConfigOption.Base;
import se.sics.ktoolbox.util.config.KConfigOption.Basic;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicAddressOption extends Base<BasicAddress> {
    private static final Logger LOG = LoggerFactory.getLogger("KConfig");
    
    public BasicAddressOption(String optName) {
        super(optName, BasicAddress.class);
    }

    @Override
    public Optional<BasicAddress> readValue(Config config) {
        InetAddressOption ipOpt = new InetAddressOption(name + ".ip");
        Optional<InetAddress> ip = ipOpt.readValue(config);
        if (!ip.isPresent()) {
            LOG.debug("missing:{}", ipOpt.name);
            return Optional.absent();
        }
        Basic<Integer> portOpt = new Basic(name + ".port", Integer.class);
        Optional<Integer> port = portOpt.readValue(config);
        if (!port.isPresent()) {
            LOG.debug("missing:{}", portOpt.name);
            return Optional.absent();
        }
        Basic<Integer> idOpt = new Basic(name + ".id", Integer.class);
        Optional<Integer> id = idOpt.readValue(config);
        if (!id.isPresent()) {
            LOG.debug("missing:{}", idOpt.name);
            return Optional.absent();
        }
        Identifier nodeId = BasicIdentifiers.nodeId(new BasicBuilders.IntBuilder(id.get()));
        BasicAddress adr = new BasicAddress(ip.get(), port.get(), nodeId);
        return Optional.of(adr);
    }
}
