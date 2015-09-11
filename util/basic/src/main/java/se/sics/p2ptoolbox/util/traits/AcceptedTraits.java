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
package se.sics.p2ptoolbox.util.traits;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.javatuples.Pair;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AcceptedTraits {

    private final ImmutableMap<Class<? extends Trait>, Integer> acceptedTraits;

    public AcceptedTraits() {
        this(ImmutableMap.<Class<? extends Trait>, Integer>of());
    }
    public AcceptedTraits(ImmutableMap<Class<? extends Trait>, Integer> acceptedTraits) {
        this.acceptedTraits = acceptedTraits;
    }

    public boolean acceptedTrait(Class<? extends Trait> traitClass) {
        return acceptedTraits.containsKey(traitClass);
    }

    public <T extends Trait> TraitInfo<T> getTraitInfo(Class<T> trait) {
        Integer traitIndex = acceptedTraits.get(trait);
        return new TraitInfo(traitIndex, trait);
    }
    
    public int getIndex(Class trait) {
        return acceptedTraits.get(trait);
    }

    public int size() {
        return acceptedTraits.size();
    }
    
    public int getIdSize() {
        return (int)Math.ceil(Math.log(acceptedTraits.size() + 1));
    }

    public static class TraitInfo<T extends Trait> {

        public final int index;
        public final Class<T> traitClass;

        public TraitInfo(Integer index, Class<T> traitClass) {
            this.index = index;
            this.traitClass = traitClass;
        }

        @Override
        public String toString() {
            return traitClass.toString();
        }
    }
}
