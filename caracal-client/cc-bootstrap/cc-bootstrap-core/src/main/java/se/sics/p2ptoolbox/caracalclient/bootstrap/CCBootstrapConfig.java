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

package se.sics.p2ptoolbox.caracalclient.bootstrap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCBootstrapConfig {
    private final static Logger log = LoggerFactory.getLogger(CCBootstrapComp.class);
    
    public final long sanityCheckTimeout;
    public final long caracalTimeout;
    public final int msgRetries;
    public final int nodeListSize;
    public final int sampleSize;
    
    public CCBootstrapConfig(long sanityCheckTimeout, long caracalTimeout, int msgRetries, int caracalNodeSize, int sampleSize) {
        this.sanityCheckTimeout = sanityCheckTimeout;
        this.caracalTimeout = caracalNodeSize;
        this.msgRetries = msgRetries;
        this.nodeListSize = caracalNodeSize;
        this.sampleSize = sampleSize;
    }
    
    public CCBootstrapConfig(Config config) {
        try{
            this.sanityCheckTimeout = config.getLong("caracal-client.bootstrap.sanityCheckTimeout");
            this.caracalTimeout = config.getLong("caracal-client.bootstrap.caracalTimeout");
            this.msgRetries = config.getInt("caracal-client.bootstrap.msgRetries");
            this.nodeListSize = config.getInt("caracal-client.bootstrap.nodeListSize");
            this.sampleSize = config.getInt("caracal-client.bootstrap.sampleSize");
        } catch(ConfigException.Missing ex) {
            log.error("{} CCBootstrap configuration problem:{}", ex.getMessage());
            throw new RuntimeException("CCBootstrap configuration problem", ex);
        }
    }
}
