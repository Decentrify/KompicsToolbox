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
package se.sics.ktoolbox.cc.bootstrap.util;

import com.google.common.base.Optional;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Address;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.KConfigOption;
import se.sics.ktoolbox.util.config.options.InetAddressOption;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CaracalAddressOption extends KConfigOption.Base<Address> {

    private static final Logger LOG = LoggerFactory.getLogger("KConfig");

    public CaracalAddressOption(String optName) {
        super(optName, Address.class);
    }

    @Override
    public Optional<Address> readValue(Config config) {
        InetAddressOption ipOpt = new InetAddressOption(name + ".ip");
        Optional<InetAddress> ip = ipOpt.readValue(config);
        if (!ip.isPresent()) {
            LOG.debug("missing:{}", ipOpt.name);
            return Optional.absent();
        }
        KConfigOption.Basic<Integer> portOpt = new KConfigOption.Basic(name + ".port", Integer.class);
        Optional<Integer> port = portOpt.readValue(config);
        if (!port.isPresent()) {
            LOG.debug("missing:{}", portOpt.name);
            return Optional.absent();
        }
        Address adr = new Address(ip.get(), port.get(), null);
        return Optional.of(adr);
    }
}
