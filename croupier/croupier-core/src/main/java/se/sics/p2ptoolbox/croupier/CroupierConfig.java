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
 * You should have received a copy of the GNUs General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.p2ptoolbox.croupier;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierConfig {

    private final static Logger log = LoggerFactory.getLogger(CroupierConfig.class);

    public final CroupierSelectionPolicy policy;
    public final int viewSize;
    public final int shuffleSize;
    public final int shufflePeriod;
    public final int shuffleTimeout;
    public final double softMaxTemperature;

    public CroupierConfig(Config config) {
        try {
            this.policy = CroupierSelectionPolicy.create(config.getString("croupier.policy"));
            this.viewSize = config.getInt("croupier.viewSize");
            this.shuffleSize = config.getInt("croupier.shuffleSize");
            this.shufflePeriod = config.getInt("croupier.shufflePeriod");
            this.shuffleTimeout = config.getInt("croupier.shuffleTimeout");
            if(shufflePeriod < shuffleTimeout) {
                log.error("shuffle period should be larger than shuffle timeout");
                throw new RuntimeException("shufflePeriod / shuffleTimeout missconfiguration");
            }
            this.softMaxTemperature = config.getDouble("croupier.softMaxTemperature");
            log.info("policy:{} view size:{} shuffle size:{} period:{} timeout:{} softMaxTemperature:{}", 
                    new Object[]{policy, viewSize, shuffleSize, shufflePeriod, shuffleTimeout, softMaxTemperature});
        } catch (ConfigException.Missing ex) {
            log.error("missing parameter", ex);
            throw new RuntimeException(ex);
        }
    }
}
