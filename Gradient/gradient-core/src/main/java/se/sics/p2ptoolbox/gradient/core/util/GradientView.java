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
package se.sics.p2ptoolbox.gradient.core.util;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
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
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.gradient.api.GradientFilter;
import se.sics.p2ptoolbox.util.InvertedComparator;
import se.sics.p2ptoolbox.util.ProbabilitiesHelper;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientView {
    private static double softMaxTemp = 1;

    private final Comparator<CroupierPeerView> ageComparator;
    private final Comparator<CroupierPeerView> utilityComp;
    private final GradientFilter filter;

    private final int viewSize;
    private final Random rand;

    private final Map<VodAddress, CroupierPeerView> view;

    public GradientView(final Comparator<PeerView> utilityComparator, GradientFilter filter, int viewSize, Random rand) {
        //TODO Alex replace with a WrapperComparator once CroupierPeerView implements ComparableWrapper
        this.utilityComp = new Comparator<CroupierPeerView>() {
            public int compare(CroupierPeerView o1, CroupierPeerView o2) {
                double compareToValue = Math.signum(utilityComparator.compare(o1.pv, o2.pv));
                if (compareToValue == 0) {
                    //should use CroupierPeerView compareTo to be equal consistent
                    return (int) compareToValue;
                }
                return (int) compareToValue;
            }
        };
        // The age comparator orders the nodes in descending order.
        this.ageComparator = new Comparator<CroupierPeerView>() {

            public int compare(CroupierPeerView o1, CroupierPeerView o2) {
                if (o1.getAge() == o2.getAge()) {
                    utilityComp.compare(o1, o2);
                }
                return (o1.getAge() > o2.getAge() ? -1 : 1);
            }
        };
        this.filter = filter;
        this.view = new HashMap<VodAddress, CroupierPeerView>();
        this.viewSize = viewSize;
        this.rand = rand;
    }

    public boolean isEmpty() {
        return view.isEmpty();
    }

    public void incrementAges() {
        for (CroupierPeerView cpv : view.values()) {
            cpv.incrementAge();
        }
    }
    
    public void clean(PeerView selfPV) {
        
        Iterator<Map.Entry<VodAddress, CroupierPeerView>> iterator = view.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<VodAddress,CroupierPeerView> entry = iterator.next();
            if(!filter.retainOther(selfPV, entry.getValue().pv)){
                iterator.remove();
            }
        }
    }

    public void merge(ImmutableCollection<CroupierPeerView> newSample, CroupierPeerView selfCPV) {
        for (CroupierPeerView cpv : newSample) {
            if (cpv.src.equals(selfCPV.src)) {
                continue;
            }
            if(!filter.retainOther(selfCPV.pv, cpv.pv)) {
                continue;
            }
            CroupierPeerView currentCPV = view.get(cpv.src);
            if (currentCPV != null) {
                if (currentCPV.getAge() > cpv.getAge()) {
                    view.put(cpv.src, cpv);
                }
            } else {
                view.put(cpv.src, cpv);
            }
        }
        if (view.size() > viewSize) {
            softMaxReduceSize(ageComparator, 1);
        }
        if (view.size() > viewSize) {
            softMaxReduceSize(new GradientPreferenceComparator<CroupierPeerView>(selfCPV, utilityComp), view.size() - viewSize);
        }
    }

    public CroupierPeerView getShuffleNode(CroupierPeerView selfCPV) {
        if (view.isEmpty()) {
            return null;
        }

        int shuffleNodeIndex = ProbabilitiesHelper.getSoftMaxVal(view.size(), rand, softMaxTemp);
        List<CroupierPeerView> sortedList = new ArrayList<CroupierPeerView>(view.values());
        Comparator<CroupierPeerView> selfPrefferenceComparator = new InvertedComparator<CroupierPeerView>(new GradientPreferenceComparator<CroupierPeerView>(selfCPV, utilityComp));
        Collections.sort(sortedList, selfPrefferenceComparator);

        return sortedList.get(shuffleNodeIndex);
    }

    public ImmutableCollection<CroupierPeerView> getExchangeCPV(CroupierPeerView partnerCPV, int n) {
        Comparator<CroupierPeerView> partnerPrefferenceComparator = new InvertedComparator<CroupierPeerView>(new GradientPreferenceComparator<CroupierPeerView>(partnerCPV, utilityComp));
        ImmutableCollection<CroupierPeerView> immutableCollection;
        
        int size = view.values().size();
        if(size <= n){
            immutableCollection = Ordering.from(partnerPrefferenceComparator).immutableSortedCopy(view.values());    
        }
        else{
            immutableCollection = Ordering.from(partnerPrefferenceComparator).immutableSortedCopy(view.values()).subList(0, n);
        }
        return immutableCollection;
    }

    public ImmutableCollection<CroupierPeerView> getView() {
        return Ordering.from(utilityComp).immutableSortedCopy(view.values());
    }

    public void clean(VodAddress node) {
        view.remove(node);
    }

    /**
     *  Based on the comparator supplied, sort entries and then reduce the size of the view using SoftMax Approach.
     * @param usedComparator Comparator used for sorting.
     * @param n Number of samples to remove.
     */
    private void softMaxReduceSize(Comparator<CroupierPeerView> usedComparator, int n) {
        List<CroupierPeerView> sortedList = new ArrayList<CroupierPeerView>(view.values());
        Collections.sort(sortedList, usedComparator);

        while (n > 0 && !sortedList.isEmpty()) {
            n--;
            CroupierPeerView toRemove = sortedList.remove(ProbabilitiesHelper.getSoftMaxVal(view.size(), rand, softMaxTemp)); //remove from ist as well so as not to pick it again
            view.remove(toRemove.src);
        }
    }
}
