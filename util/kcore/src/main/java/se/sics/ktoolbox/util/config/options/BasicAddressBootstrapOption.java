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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.simutil.msg.impl.BasicAddress;
import se.sics.ktoolbox.util.config.KConfigCore;
import se.sics.ktoolbox.util.config.KConfigOption.Basic;
import se.sics.ktoolbox.util.config.KConfigOption.Composite;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicAddressBootstrapOption extends Composite<List> {

    private static final Logger LOG = LoggerFactory.getLogger("KConfig");

    public BasicAddressBootstrapOption(String optName) {
        super(optName, List.class);
    }

    @Override
    public Optional<List> readValue(KConfigCore config) {
        String sPartnersOpt = name + ".partners";
        Basic<List> partnersOpt = new Basic(sPartnersOpt, List.class);
        Optional<List> partners = config.readValue(partnersOpt);
        if (!partners.isPresent()) {
            LOG.debug("missing partners:{}", sPartnersOpt);
            return Optional.absent();
        }
        List<BasicAddress> partnerAdr = new ArrayList<>();
        for(String partner : (List<String>)partners.get()) {
            String sAdrOpt = name + "." + partner;
            BasicAddressOption adrOpt = new BasicAddressOption(sAdrOpt);
            Optional<BasicAddress> adr = config.readValue(adrOpt);
            if(adr.isPresent()) {
                partnerAdr.add(adr.get());
            } else {
                LOG.warn("partner:{} improperly defined", sAdrOpt);
            }
        }
        return Optional.of((List)partnerAdr);
    }
}
