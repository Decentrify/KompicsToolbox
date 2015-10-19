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
import java.net.InetAddress;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigLevel;
import se.sics.p2ptoolbox.util.config.KConfigOption.Basic;
import se.sics.p2ptoolbox.util.config.KConfigOption.Composite;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OpenAddressOption extends Composite<DecoratedAddress> {
    private final InetAddressOption ipOpt;
    private final Basic<Integer> portOpt;
    private final Basic<Integer> idOpt;
    
    public OpenAddressOption(String optName, KConfigLevel optLvl, InetAddressOption ipOpt, Basic<Integer> portOpt, Basic<Integer> idOpt) {
        super(optName, DecoratedAddress.class, optLvl);
        this.ipOpt = ipOpt;
        this.portOpt = portOpt;
        this.idOpt = idOpt;
    }

    @Override
    public Optional<DecoratedAddress> read(KConfigCache config) {
        Optional<InetAddress> ip = config.read(ipOpt);
        Optional<Integer> port = config.read(portOpt);
        Optional<Integer> id = config.read(idOpt);
        if(!(ip.isPresent() && port.isPresent() && id.isPresent())) {
            return Optional.absent();
        }
        DecoratedAddress adr = DecoratedAddress.open(ip.get(), port.get(), id.get());
        return Optional.of(adr);
    }
}
