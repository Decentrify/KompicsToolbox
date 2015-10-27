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
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public final class KConfigOption {

    public static abstract class Base<T extends Object> {

        public final String name;
        public final Class<T> type;
        public final KConfigLevel lvl;

        public Base(String name, Class<T> type, KConfigLevel lvl) {
            this.name = name;
            this.type = type;
            this.lvl = lvl;
        }

        @Override
        public String toString() {
            return name + " lvl:" + lvl + " type" + type;
        }
    }
    
    public static final class Basic<T extends Object> extends Base<T> {
        public Basic(String name, Class<T> type, KConfigLevel lvl) {
            super(name, type, lvl);
        }
    }

    public static abstract class Composite<T extends Object> extends Base<T>{

        public Composite(String name, Class<T> type, KConfigLevel lvl) {
            super(name, type, lvl);
        }

        @Deprecated
        public abstract Optional<T> read(KConfigCache config);
        
        //TODO Alex fix later to abstract
        public Optional<T> readValue(KConfigCore config) {
            throw new UnsupportedOperationException("should have overwritten this method - not abstract for backward compatibility");
        }
    }
}
