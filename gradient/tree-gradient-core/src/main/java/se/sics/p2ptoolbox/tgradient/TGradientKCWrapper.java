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
package se.sics.p2ptoolbox.tgradient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TGradientKCWrapper {

    private final static Logger LOG = LoggerFactory.getLogger("KConfig");
    private String logPrefix = "";

    public final KConfigCache config;
    public final int centerNodes;
    public final int branching;

    public TGradientKCWrapper(KConfigCore configCore) {
        this.config = new KConfigCache(configCore);
        this.logPrefix = "<" + config.getNodeId() + ">tgradient:";
        this.centerNodes = KConfigHelper.read(config, TGradientKConfig.centerNodes, LOG, logPrefix);
        this.branching = KConfigHelper.read(config, TGradientKConfig.branching, LOG, logPrefix);
    }
}
