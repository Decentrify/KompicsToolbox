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
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Address;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption;
import se.sics.p2ptoolbox.util.config.KConfigOption.Basic;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CaracalAddressOption extends KConfigOption.Composite<Address> {

    private static final Logger LOG = LoggerFactory.getLogger("KConfig");

    public CaracalAddressOption(String optName, KConfigLevel optLvl) {
        super(optName, Address.class, optLvl);
    }

    @Override
    public Optional<Address> readValue(KConfigCore config) {
        Basic<String> ipOpt = new Basic(name + ".ip", String.class, lvl);
        Optional<String> ip = config.readValue(ipOpt);
        if (!ip.isPresent()) {
            LOG.debug("missing:{}", ipOpt.name);
            return Optional.absent();
        }
        Basic<Integer> portOpt = new Basic(name + ".port", Integer.class, lvl);
        Optional<Integer> port = config.readValue(portOpt);
        if (!port.isPresent()) {
            LOG.debug("missing:{}", portOpt.name);
            return Optional.absent();
        }
        Address adr;
        try {
            adr = new Address(InetAddress.getByName(ip.get()), port.get(), null);
        } catch (UnknownHostException ex) {
            LOG.error("ip error:{}", ex.getMessage());
            throw new RuntimeException(ex);
        }
        return Optional.of(adr);
    }
}