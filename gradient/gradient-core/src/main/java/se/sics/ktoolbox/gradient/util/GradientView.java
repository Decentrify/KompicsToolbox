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
package se.sics.ktoolbox.gradient.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.network.Address;
import se.sics.kompics.simutil.identifiable.Identifiable;
import se.sics.kompics.simutil.identifiable.Identifier;
import se.sics.ktoolbox.gradient.GradientComp;
import se.sics.ktoolbox.gradient.GradientFilter;
import se.sics.ktoolbox.util.InvertedComparator;
import se.sics.ktoolbox.util.ProbabilitiesHelper;
import se.sics.ktoolbox.util.compare.WrapperComparator;
import se.sics.ktoolbox.gradient.GradientKCWrapper;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientView {

    private static final Logger log = LoggerFactory.getLogger(GradientComp.class);
    private final String logPrefix;
    
    private final Comparator<GradientContainer> ageComparator;
    private final Comparator<GradientContainer> utilityComp;
    private final GradientFilter filter;

    private final GradientKCWrapper config;
    private final Random rand;

    private final Map<Identifier, GradientContainer> view;

    public GradientView(GradientKCWrapper config, String logPrefix, Comparator utilityComparator, GradientFilter filter) {
        this.config = config;
        this.logPrefix = logPrefix;
        this.utilityComp = new WrapperComparator<>(utilityComparator);
        this.ageComparator = new InvertedComparator<>(new GradientContainerAgeComparator()); //we want old ages in the begining
        this.filter = filter;
        this.view = new HashMap<>();
        this.rand = new Random(config.seed);
    }

    public boolean isEmpty() {
        return view.isEmpty();
    }

    public void incrementAges() {
        for (GradientContainer gc : view.values()) {
            gc.incrementAge();
        }
    }

    public void clean(Object selfView) {

        Iterator<Map.Entry<Identifier, GradientContainer>> iterator = view.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Identifier, GradientContainer> entry = iterator.next();
            if (!filter.retainOther(selfView, entry.getValue().getContent())) {
                iterator.remove();
            }
        }
    }

    public void merge(GradientContainer newSample, GradientContainer selfView) {
        Set<GradientContainer> newSet = new HashSet<GradientContainer>();
        newSet.add(newSample);
        merge(newSet, selfView);
    }

    public void merge(Collection<GradientContainer> newSample, GradientContainer selfView) {
        for (GradientContainer gc : newSample) {
            if (((Identifiable)gc).getId().equals(((Identifiable)selfView).getId())) {
                continue;
            }
            if (!filter.retainOther(selfView.getContent(), gc.getContent())) {
                //The Filter says the new descriptor should not be in the view.
                //If we have an old descriptor in the view, we should remove it.
                view.remove(((Identifiable)gc).getId());
                continue;
            }
            GradientContainer currentGc = view.get(((Identifiable)gc).getId());
            if (currentGc != null) {
                if (currentGc.getAge() > gc.getAge()) {
                    view.put(((Identifiable)gc).getId(), gc);
                }
            } else {
                view.put(((Identifiable)gc).getId(), gc);
            }
        }
        log.debug("{} remove - before shrink:{}", new Object[]{logPrefix, view.values()});
        //Should enable cleaning old descriptors even when view is incomplete
        //Even if this cleanup empties the view, we can wait for Croupier to provide new samples
        //We should only remove descriptors older than a defined threshold so we don't disconnect
//        if (view.size() > configCore.viewSize) {
        for (GradientContainer toRemove : reduceSize(ageComparator, 1)) {
            if (toRemove.getAge() >= config.oldThreshold) {
                log.debug("{} remove - old:{}", new Object[]{logPrefix, toRemove});
                view.remove(((Identifiable)toRemove).getId());
            }
        }
//        }
        if (view.size() > config.viewSize) {
            GradientPreferenceComparator<GradientContainer> preferenceComparator = new GradientPreferenceComparator<GradientContainer>(selfView, utilityComp);
            int reduceSize = view.size() - config.viewSize;
            for (GradientContainer toRemove : reduceSize(preferenceComparator, reduceSize)) {
                log.debug("{} remove - self:{} preference bad:{}", new Object[]{logPrefix, selfView, toRemove});
                view.remove(((Identifiable)toRemove).getId());
            }
        }
        log.debug("{} remove - after shrink:{}", logPrefix, view.values());
    }

    public GradientContainer getShuffleNode(GradientContainer selfCPV) {
        if (view.isEmpty()) {
            return null;
        }

        int shuffleNodeIndex = ProbabilitiesHelper.getSoftMaxVal(view.size(), rand, config.softMaxTemp);
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Comparator<GradientContainer> selfPrefferenceComparator = new InvertedComparator<GradientContainer>(new GradientPreferenceComparator<GradientContainer>(selfCPV, utilityComp));
        Collections.sort(sortedList, selfPrefferenceComparator);

        return sortedList.get(shuffleNodeIndex);
    }

    public Set<GradientContainer> getExchangeCopy(GradientContainer partnerCPV, int n) {
        Comparator<GradientContainer> partnerPrefferenceComparator = new InvertedComparator<GradientContainer>(new GradientPreferenceComparator<GradientContainer>(partnerCPV, utilityComp));
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Collections.sort(sortedList, partnerPrefferenceComparator);
        Set<GradientContainer> copySet = new HashSet<GradientContainer>();
        for (int i = 0; i < n && i < sortedList.size(); i++) {
            copySet.add(sortedList.get(i).getCopy());
        }
        return copySet;
    }

    public List<GradientContainer> getAllCopy() {
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Collections.sort(sortedList, utilityComp);
        List<GradientContainer> copyList = new ArrayList<GradientContainer>();
        for (GradientContainer gc : sortedList) {
            copyList.add(gc.getCopy());
        }
        return copyList;
    }

    public void clean(Address node) {
        view.remove(((Identifiable)node).getId());
    }

    public boolean checkIfTop(GradientContainer selfView) {
        if (view.size() < config.viewSize) {
            return false;
        }
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Collections.sort(sortedList, utilityComp);
        return utilityComp.compare(selfView, sortedList.get(sortedList.size() - 1)) > 0;
    }

    public int getDistTo(GradientContainer selfView, GradientContainer targetView) {
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Collections.sort(sortedList, utilityComp);
        int dist = 0;
        for (GradientContainer gc : sortedList) {
            if (utilityComp.compare(selfView, gc) >= 0) {
                continue;
            }
            if (utilityComp.compare(targetView, gc) < 0) {
                break;
            }
            dist++;
        }
        return dist;
    }

    /**
     * @param usedComparator Comparator used for sorting.
     * @param n Number of samples to remove.
     */
    private List<GradientContainer> reduceSize(Comparator<GradientContainer> usedComparator, int n) {
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Collections.sort(sortedList, usedComparator);
        List<GradientContainer> toRemove = new ArrayList<GradientContainer>();
        while (n > 0 && !sortedList.isEmpty()) {
            n--;
            toRemove.add(sortedList.remove(0)); //remove from list so as not to pick it again
        }
        return toRemove;
    }
}
