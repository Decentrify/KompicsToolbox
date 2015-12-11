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
package se.sics.ktoolbox.util.config;

import com.google.common.base.Optional;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.HashMap;
import java.util.Map;
import org.javatuples.Pair;
import se.sics.ktoolbox.util.config.KConfigOption.Basic;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class KConfigCore {
    private final Config config;
    private final Map<String, Pair<KConfigOption.Base, Object>> options = new HashMap<>();

    public KConfigCore(Config config) {
        this.config = config;
    }

    public synchronized <T extends Object> Optional<T> readValue(KConfigOption.Base<T> option) throws IllegalArgumentException {
        Pair<KConfigOption.Base, Object> existingOV = options.get(option.name);
        if (existingOV != null) {
            if (!existingOV.getValue0().type.equals(option.type)) {
                String msg = "mismatched option:" + option.name + " type - ";
                msg += "expected:" + option.type + " found:" + existingOV.getValue0().type;
                throw new IllegalArgumentException(msg);
            }
        } else {
            Object optionValue;
            try {
                if (option instanceof Basic) {
                    if (option.type.equals(String.class)) {
                        optionValue = config.getString(option.name);
                    } else if (option.type.equals(Integer.class)) {
                        optionValue = config.getInt(option.name);
                    } else if (option.type.equals(Long.class)) {
                        optionValue = config.getLong(option.name);
                    } else if (option.type.equals(Double.class)) {
                        optionValue = config.getDouble(option.name);
                    } else if (option.type.equals(Boolean.class)) {
                        optionValue = config.getBoolean(option.name);
                    } else {
                        optionValue = config.getStringList(option.name);
                    }
                } else {
                    //composite
                    KConfigOption.Composite<T> opt = (KConfigOption.Composite<T>)option;
                    Optional<T> aux = opt.readValue(this);
                    if(!aux.isPresent()) {
                        return Optional.absent();
                    }
                    optionValue = aux.get();
                }
                existingOV = Pair.with((KConfigOption.Base) option, optionValue);
                options.put(option.name, existingOV);
            } catch (ConfigException.Missing ex) {
                return Optional.absent();
            }
        }
        return Optional.of((T) existingOV.getValue1());
    }

    @Deprecated
    public synchronized <T extends Object> void write(KConfigOption.Basic<T> option, T value) {
    }

    public synchronized <T extends Object> void writeValue(KConfigOption.Base<T> option, T value) {
        Pair<KConfigOption.Base, Object> existingOV = options.get(option.name);
        if (existingOV != null) {
            throw new RuntimeException("double write of option:" + option.name);
        }
        options.put(option.name, Pair.with((KConfigOption.Base) option, (Object) value));
    }    
}
