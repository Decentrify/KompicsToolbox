/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class TreeGradientConfig {
    public final int kCenterNodes;
    public final int branching;

    public TreeGradientConfig(Config config) {
        try {
            this.kCenterNodes = config.getInt("treegradient.kCenterNodes");
            this.branching = config.getInt("treegradient.branching");
        } catch (ConfigException.Missing ex) {
//            log.error("missing configuration parameter - {}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
