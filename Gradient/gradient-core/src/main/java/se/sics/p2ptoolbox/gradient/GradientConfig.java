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
package se.sics.p2ptoolbox.gradient;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Boot up configuration for the Gradient.
 * 
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientConfig {
    
    private final static Logger log = LoggerFactory.getLogger(GradientComp.class);
    
    public final int viewSize;
    public final int shuffleSize;
    public final int shufflePeriod;
    public final int shuffleTimeout;
    public final double exchangeSMTemp;

    public GradientConfig(Config config) {
        try {
            this.viewSize = config.getInt("gradient.viewSize");
            this.shuffleSize = config.getInt("gradient.shuffleSize");
            this.shufflePeriod = config.getInt("gradient.shufflePeriod");
            this.shuffleTimeout = config.getInt("gradient.shuffleTimeout");
            if(shufflePeriod < shuffleTimeout) {
                log.error("shuffle period should be larger than shuffle timeout");
                throw new RuntimeException("shufflePeriod / shuffleTimeout missconfiguration");
            }
            this.exchangeSMTemp = config.getDouble("gradient.exchangeSMTemp");
            log.info("view size:{} shuffle size:{} period:{} timeout:{} exchangeSMTemp:{}", 
                    new Object[]{viewSize, shuffleSize, shufflePeriod, shuffleTimeout, exchangeSMTemp});
        } catch (ConfigException.Missing ex) {
            log.error("missing configuration parameter - {}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
    
    public GradientConfig(int viewSize, int shuffleSize, int shufflePeriod, int shuffleTimeout, double exchangeSMTemp){
        this.viewSize = viewSize;
        this.shuffleSize = shuffleSize;
        this.shufflePeriod = shufflePeriod;
        this.shuffleTimeout = shuffleTimeout;
        this.exchangeSMTemp = exchangeSMTemp;
    }
}
