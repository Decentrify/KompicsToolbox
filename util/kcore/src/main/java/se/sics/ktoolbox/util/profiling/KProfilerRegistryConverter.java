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
package se.sics.ktoolbox.util.profiling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Converter;
import se.sics.ktoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KProfilerRegistryConverter implements Converter<KProfilerRegistry> {

    private static final Logger LOG = LoggerFactory.getLogger("KConfig");

    @Override
    public KProfilerRegistry convert(Object o) {
        if (o instanceof Map) {
            Map<String, String> registryMap = (Map)o;
            Map<String, KProfiler.Type> registry = new HashMap<>();
            for(Map.Entry<String, String> e : registryMap.entrySet()) {
                registry.put(e.getKey(), KProfiler.Type.valueOf(e.getValue()));
            }
            return new KProfilerRegistry(registry);
        } else if (o instanceof KProfilerRegistry) {
            return (KProfilerRegistry) o;
        } else {
            throw new RuntimeException("unknown");
        }
    }

    @Override
    public Class<KProfilerRegistry> type() {
        return KProfilerRegistry.class;
    }

    public static String jsonPrettyPrint(KProfilerRegistry registry) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(registry.registry);
    }

    private static class Entry {

        private String component;
        private String lvl;

        public Entry() {
        }

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public String getLvl() {
            return lvl;
        }

        public void setLvl(String lvl) {
            this.lvl = lvl;
        }
    }
}
