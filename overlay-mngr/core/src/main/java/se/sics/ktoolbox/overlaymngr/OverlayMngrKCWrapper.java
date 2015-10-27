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
package se.sics.ktoolbox.overlaymngr;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.KConfigHelper;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayMngrKCWrapper {
    private final static Logger LOG = LoggerFactory.getLogger("KConfig");
    private String logPrefix = "";
    
    private final KConfigCache config;
    public final long seed;
    public final List<DecoratedAddress> bootstrap;
    
    public OverlayMngrKCWrapper(KConfigCore configCore) {
        this.config = new KConfigCache(configCore);
        this.logPrefix = config.getNodeId() + " ";
        this.seed = KConfigHelper.read(config, OverlayMngrConfig.seed, LOG, logPrefix);
        this.bootstrap = KConfigHelper.read(config, OverlayMngrConfig.bootstrap, LOG, logPrefix);
    }
    
    public KConfigCore getConfigCore() {
        return config.configCore;
    }
}
