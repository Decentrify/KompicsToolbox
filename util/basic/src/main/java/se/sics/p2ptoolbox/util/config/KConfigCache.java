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

import com.google.common.base.Optional;
import com.typesafe.config.ConfigException;
import java.util.HashMap;
import java.util.Map;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KConfigCache {

    public final KConfigCore configCore;
    private final Map<String, Pair<KConfigOption.Base, Object>> options = new HashMap<>();

    public KConfigCache(KConfigCore config) {
        this.configCore = config;
    }

    public int getNodeId() {
        return configCore.getNodeId();
    }

    public <T extends Object> Optional<T> read(KConfigOption.Base<T> option) {
        if (option instanceof KConfigOption.Composite) {
            if (!options.containsKey(option.name)) {
                return ((KConfigOption.Composite<T>) option).read(this);
            } else {
                return Optional.of((T) (options.get(option.name).getValue1()));
            }
        } else {
            KConfigOption.Basic<T> op = (KConfigOption.Basic<T>) option;
            T value;
            if (!options.containsKey(op.name)) {
                try {
                    value = configCore.read(op);
                } catch (ConfigException.Missing ex) {
                    return Optional.absent();
                }
                options.put(option.name, Pair.with((KConfigOption.Base) op, (Object) value));
            } else {
                value = (T) options.get(op.name).getValue1();
            }
            return Optional.of(value);
        }
    }

    public <T extends Object> void write(KConfigOption.Base<T> option, T value) {
        if (option instanceof KConfigOption.Composite) {
            throw new RuntimeException("cannot write composite options");
        }
        configCore.write((KConfigOption.Basic<T>) option, value);
        options.put(option.name, Pair.with((KConfigOption.Base) option, (Object) value));
    }

    public void reset() {
        options.clear();
    }

    public <T extends Object> Optional<T> reset(KConfigOption.Base<T> option) {
        options.remove(option.name);
        return read(option);
    }
}
