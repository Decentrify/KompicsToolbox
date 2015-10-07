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

import java.util.HashMap;
import java.util.Map;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConfigCache {

    public final ConfigCore configCore;
    private final Map<String, Pair<ConfigOption, Object>> options = new HashMap<>();

    public ConfigCache(ConfigCore config) {
        this.configCore = config;
    }

    private int getNodeId() {
        return configCore.getNodeId();
    }

    public <T extends Class> T getOptionValue(ConfigOption<T> option) {
        T value;
        if (!options.containsKey(option.name)) {
            value = configCore.getOptionValue(option);
            options.put(option.name, Pair.with((ConfigOption) option, (Object) value));
        } else {
            value = (T) options.get(option.name).getValue1();
        }
        return value;
    }

    public <T extends Class> void setOptionValue(ConfigOption<T> option, T value) {
        configCore.setOptionValue(option, value);
    }
    
    public void resetCache() {
        options.clear();
    }
    
    public <T extends Class> T resetCache(ConfigOption<T> option) {
        options.remove(option.name);
        return getOptionValue(option);
    }
}
