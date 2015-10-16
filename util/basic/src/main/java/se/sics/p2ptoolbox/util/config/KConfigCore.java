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
package se.sics.p2ptoolbox.util.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.HashMap;
import java.util.Map;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KConfigCore {

    private static final Logger LOG = LoggerFactory.getLogger(KConfigCore.class);
    private String logPrefix = "";

    private final Config config;
    private final int nodeId;
    private final Map<String, Pair<KConfigOption.Basic, Object>> options = new HashMap<>();

    public KConfigCore(Config config) {
        this.config = config;
        this.nodeId = readNodeId();
        this.logPrefix = nodeId + " ";
    }

    public KConfigCore(Config config, int nodeId) {
        this.config = config;
        this.nodeId = nodeId;
        this.logPrefix = nodeId + " ";
    }

    public int getNodeId() {
        return nodeId;
    }

    private int readNodeId() {
        try {
            return config.getInt("system.address.id");
        } catch (ConfigException.Missing ex) {
            LOG.error("{}missing node id", logPrefix);
            throw new RuntimeException(ex);
        }
    }

    public synchronized void define(KConfigOption.Basic option) {
        if (options.containsKey(option.name)) {
            LOG.error("{}config error, double option:{}", logPrefix, option.name);
            throw new RuntimeException("config error, double option:" + option.name);
        }
        options.put(option.name, Pair.with(option, null));
    }

    public synchronized <T extends Object> T read(KConfigOption.Basic<T> option) throws ConfigException.Missing {
        Pair<KConfigOption.Basic, Object> existingOV = options.get(option.name);
        if (existingOV == null) {
            LOG.error("{}config error, undefined option:{}", logPrefix, option.name);
            throw new RuntimeException("config error, undefined option:" + option.name);
        }
        if (existingOV.getValue1() == null) {
            existingOV = Pair.with((KConfigOption.Basic) existingOV.getValue0(), config.getAnyRef(option.name));
            options.put(option.name, existingOV);
        }
        return (T) existingOV.getValue1();
    }

    public synchronized <T extends Object> void write(KConfigOption.Basic<T> option, T value) {
        if (!options.containsKey(option.name)) {
            LOG.error("{}config error, undefined option:{}", logPrefix, option.name);
            throw new RuntimeException("config error, undefined option:" + option.name);
        }
        Pair<KConfigOption.Basic, Object> existingOV = options.get(option.name);
        if (!option.lvl.canWrite().contains(existingOV.getValue0().lvl.toString())) {
            LOG.error("{}config error, unsanctioned write:{}", logPrefix, option);
            throw new RuntimeException("config error, unsanctioned write:" + option);
        }
        options.put(option.name, Pair.with((KConfigOption.Basic) existingOV.getValue0(), (Object) value));
    }
}
