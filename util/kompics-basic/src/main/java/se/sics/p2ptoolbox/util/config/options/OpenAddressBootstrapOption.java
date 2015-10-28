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
package se.sics.p2ptoolbox.util.config.options;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption.Basic;
import se.sics.p2ptoolbox.util.config.KConfigOption.Composite;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OpenAddressBootstrapOption extends Composite<List> {

    private static final Logger LOG = LoggerFactory.getLogger("KConfig");

    public OpenAddressBootstrapOption(String optName, KConfigLevel optLvl) {
        super(optName, List.class, optLvl);
    }

    @Deprecated
    @Override
    public Optional<List> read(KConfigCache config) {
        throw new UnsupportedOperationException("not yet removed for backward compatibility(compile)");

    }
    
    @Override
    public Optional<List> readValue(KConfigCore config) {
        Basic<List> partnersOpt = new Basic(name + ".partners", List.class, lvl);
        Optional<List> partners = config.readValue(partnersOpt);
        if (!partners.isPresent()) {
            LOG.warn("missing partners");
            return Optional.absent();
        }
        List<DecoratedAddress> partnerAdr = new ArrayList<>();
        for(String partner : (List<String>)partners.get()) {
            OpenAddressOption adrOpt = new OpenAddressOption(name + "." + partner + ".address", lvl);
            Optional<DecoratedAddress> adr = config.readValue(adrOpt);
            if(adr.isPresent()) {
                partnerAdr.add(adr.get());
            }
        }
        return Optional.of((List)partnerAdr);
    }
}
