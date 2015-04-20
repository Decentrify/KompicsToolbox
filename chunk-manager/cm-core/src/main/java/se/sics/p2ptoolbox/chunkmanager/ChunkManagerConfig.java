/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.p2ptoolbox.chunkmanager;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alidar on 10/23/14.
 */
public class ChunkManagerConfig {

    private static final Logger log = LoggerFactory.getLogger(ChunkManagerComp.class);

    public final long cleanupTimeout;
    public final int datagramUsableSize;

    public ChunkManagerConfig(long cleanupTimeout, int datagramUsableSize) {
        this.cleanupTimeout = cleanupTimeout;
        this.datagramUsableSize = datagramUsableSize;
    }

    public ChunkManagerConfig(Config config) {
        try {
            cleanupTimeout = config.getLong("chunk-manager.cleanupTimeout");
            datagramUsableSize = config.getInt("chunk-manager.datagramUsableSize");
            log.info("config - datagramUsableSize:{} cleanupTimeout:{}", datagramUsableSize, cleanupTimeout);
        } catch (ConfigException.Missing ex) {
            log.error("missing configuration:{}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
