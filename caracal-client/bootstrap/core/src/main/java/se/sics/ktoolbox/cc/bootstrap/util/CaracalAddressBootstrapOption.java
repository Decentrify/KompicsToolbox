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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Address;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.KConfigOption;
import se.sics.ktoolbox.util.config.KConfigOption.Basic;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CaracalAddressBootstrapOption extends KConfigOption.Base<List> {
    private static final Logger LOG = LoggerFactory.getLogger("KConfig");

    public CaracalAddressBootstrapOption(String optName) {
        super(optName, List.class);
    }

    @Override
    public Optional<List> readValue(Config config) {
        Basic<List> partnersOpt = new Basic(name + ".partners", List.class);
        Optional<List> partners = partnersOpt.readValue(config);
        if (!partners.isPresent()) {
            LOG.debug("missing partners");
            return Optional.absent();
        }
        List<Address> partnerAdr = new ArrayList<>();
        for(String partner : (List<String>)partners.get()) {
            CaracalAddressOption adrOpt = new CaracalAddressOption(name + "." + partner);
            Optional<Address> adr = adrOpt.readValue(config);
            if(adr.isPresent()) {
                partnerAdr.add(adr.get());
            } else {
                LOG.warn("malformed partner address:{}", name + "." + partner);
            }
        }
        return Optional.of((List)partnerAdr);
    }
}