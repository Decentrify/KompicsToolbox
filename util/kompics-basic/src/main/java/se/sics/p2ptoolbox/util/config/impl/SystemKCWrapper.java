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
package se.sics.p2ptoolbox.util.config.impl;

import com.google.common.base.Optional;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SystemKCWrapper {
    public final KConfigCore configCore;
    public final long seed;
    public final int id;
    public final int port;
    public final Optional<BasicAddress> aggregator;
    
    public SystemKCWrapper(KConfigCore configCore) {
        this.configCore = configCore;
        seed = KConfigHelper.read(configCore, SystemKConfig.seed);
        id = KConfigHelper.read(configCore, SystemKConfig.id);
        port = KConfigHelper.read(configCore, SystemKConfig.port);
        aggregator = configCore.readValue(SystemKConfig.aggregator);
    }
}
