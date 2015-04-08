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
package se.sics.p2ptoolbox.gradient.util;

import java.util.ArrayList;
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
import se.sics.p2ptoolbox.gradient.GradientComp;
import se.sics.p2ptoolbox.gradient.GradientFilter;
import se.sics.p2ptoolbox.util.InvertedComparator;
import se.sics.p2ptoolbox.util.ProbabilitiesHelper;
import se.sics.p2ptoolbox.util.compare.WrapperComparator;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientView {

    private static double shrinkSoftMaxTemperature = 1;

    private static final Logger log = LoggerFactory.getLogger(GradientComp.class);
    private final Comparator<GradientContainer> ageComparator;
    private final Comparator<GradientContainer> utilityComp;
    private final GradientFilter filter;

    private final int viewSize;
    private final Random rand;
    private final double softMaxTemp;
    private final String logPrefix;

    private final Map<Address, GradientContainer> view;

    public GradientView(String logPrefix, Comparator utilityComparator, GradientFilter filter, int viewSize, Random rand, double softMaxTemp) {
        this.utilityComp = new WrapperComparator<GradientContainer>(utilityComparator);
        this.ageComparator = new InvertedComparator<GradientContainer>(new GradientContainerAgeComparator()); //we want old ages in the begining
        this.filter = filter;
        this.view = new HashMap<Address, GradientContainer>();
        this.viewSize = viewSize;
        this.rand = rand;
        this.softMaxTemp = softMaxTemp;
        this.logPrefix = logPrefix;
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

        Iterator<Map.Entry<Address, GradientContainer>> iterator = view.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Address, GradientContainer> entry = iterator.next();
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

    public void merge(Set<GradientContainer> newSample, GradientContainer selfView) {
        for (GradientContainer gc : newSample) {
            if (gc.getSource().getBaseAddress().equals(selfView.getSource().getBaseAddress())) {
                continue;
            }
            if (!filter.retainOther(selfView.getContent(), gc.getContent())) {
                continue;
            }
            GradientContainer currentGc = view.get(gc.getSource().getBaseAddress());
            if (currentGc != null) {
                if (currentGc.getAge() > gc.getAge()) {
                    view.put(gc.getSource().getBaseAddress(), gc);
                }
            } else {
                view.put(gc.getSource().getBaseAddress(), gc);
            }
        }
        log.debug("{} remove - before shrink:{}", new Object[]{logPrefix, view.values()});
        if (view.size() > viewSize) {
            for (GradientContainer toRemove : softMaxReduceSize(ageComparator, 1)) {
                log.debug("{} remove - old:{}", new Object[]{logPrefix, toRemove});
                view.remove(toRemove.getSource().getBaseAddress());
            }
        }
        if (view.size() > viewSize) {
            GradientPreferenceComparator<GradientContainer> preferenceComparator = new GradientPreferenceComparator<GradientContainer>(selfView, utilityComp);
            int reduceSize = view.size() - viewSize;
            for (GradientContainer toRemove : softMaxReduceSize(preferenceComparator, reduceSize)) {
                log.debug("{} remove - self:{} preference bad:{}", new Object[]{logPrefix, selfView, toRemove});
                view.remove(toRemove.getSource().getBaseAddress());
            }
        }
        log.debug("{} remove - after shrink:{}", logPrefix, view.values());
    }

    public GradientContainer getShuffleNode(GradientContainer selfCPV) {
        if (view.isEmpty()) {
            return null;
        }

        int shuffleNodeIndex = ProbabilitiesHelper.getSoftMaxVal(view.size(), rand, softMaxTemp);
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

    public void clean(NatedAddress node) {
        view.remove(node.getBaseAddress());
    }

    /**
     * Based on the comparator supplied, sort entries and then reduce the size
     * of the view using SoftMax Approach.
     *
     * @param usedComparator Comparator used for sorting.
     * @param n Number of samples to remove.
     */
    private List<GradientContainer> softMaxReduceSize(Comparator<GradientContainer> usedComparator, int n) {
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Collections.sort(sortedList, usedComparator);
        List<GradientContainer> toRemove = new ArrayList<GradientContainer>();
        while (n > 0 && !sortedList.isEmpty()) {
            n--;
            toRemove.add(sortedList.remove(ProbabilitiesHelper.getSoftMaxVal(view.size(), rand, shrinkSoftMaxTemperature))); //remove from list so as not to pick it again
        }
        return toRemove;
    }
}
