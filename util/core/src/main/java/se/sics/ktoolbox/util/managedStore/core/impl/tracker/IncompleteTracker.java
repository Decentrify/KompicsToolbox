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
package se.sics.ktoolbox.util.managedStore.core.impl.tracker;

import se.sics.ktoolbox.util.managedStore.core.ComponentTracker;
import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class IncompleteTracker implements ComponentTracker {

    private final BitSet components;
    private final int nrComponents;

    private IncompleteTracker(int nrComponents) {
        this.components = new BitSet(nrComponents + 1);
        this.nrComponents = nrComponents;
    }

    @Override
    public boolean isComplete(int fromComponentNr) {
        return components.nextClearBit(fromComponentNr) == nrComponents;
    }

    /**
     * @param fromComponentNr included
     * @return
     */
    @Override
    public int nextComponentMissing(int fromComponentNr) {
        int nextComponentMissing = components.nextClearBit(fromComponentNr);
        if (nextComponentMissing >= nrComponents) {
            return -1;
        } else {
            return nextComponentMissing;
        }
    }

    @Override
    public Integer nextComponentMissing(int fromComponentNr, Set<Integer> except) {
        Set<Integer> missingComponent = nextComponentMissing(fromComponentNr, 1, except);
        if (missingComponent.isEmpty()) {
            return -1;
        } else {
            return missingComponent.iterator().next();
        }
    }

    @Override
    public Set<Integer> nextComponentMissing(int fromComponentNr, int n, Set<Integer> except) {
        Set<Integer> result = new TreeSet<Integer>();
        int nextComponentMissing = fromComponentNr;
        while (result.size() < n && nextComponentMissing < nrComponents) {
            nextComponentMissing = components.nextClearBit(nextComponentMissing);
            if (!except.contains(nextComponentMissing)) {
                result.add(nextComponentMissing);
            }
            nextComponentMissing++;
        }
        return result;
    }

    @Override
    public boolean hasComponent(int componentNr) {
        return components.get(componentNr);
    }

    @Override
    public void addComponent(int componentNr) {
        components.set(componentNr);
    }

    public static IncompleteTracker create(int nrComponents) {
        if (nrComponents > ManagedStoreHelper.MAX_BIT_SET_SIZE) {
            throw new RuntimeException("exceeding maximum block size:" + ManagedStoreHelper.MAX_BIT_SET_SIZE + " pieces");
        }
        return new IncompleteTracker(nrComponents);
    }
}
