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
import java.net.UnknownHostException;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.KConfigOption.Base;
import se.sics.ktoolbox.util.config.KConfigOption.Basic;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class InetAddressOption extends Base<InetAddress> {
    
    public InetAddressOption(String optionName) {
        super(optionName, InetAddress.class);
    }
    
    @Override
    public Optional<InetAddress> readValue(Config config) {
        Optional<String> sPrefferedInterface = config.readValue(name, String.class);
        if (!sPrefferedInterface.isPresent()) {
            return Optional.absent();
        }
        try {
            return Optional.of(InetAddress.getByName(sPrefferedInterface.get()));
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }
}
