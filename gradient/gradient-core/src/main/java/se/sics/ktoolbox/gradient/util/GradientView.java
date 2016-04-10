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
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.gradient.GradientComp;
import se.sics.ktoolbox.gradient.GradientFilter;
import se.sics.ktoolbox.util.InvertedComparator;
import se.sics.ktoolbox.util.ProbabilitiesHelper;
import se.sics.ktoolbox.util.compare.WrapperComparator;
import se.sics.ktoolbox.gradient.GradientKCWrapper;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientView {

    private static final Logger log = LoggerFactory.getLogger(GradientComp.class);
    private final String logPrefix;

    private final Comparator<GradientContainer> ageComparator;
    private final Comparator<GradientContainer> utilityComp;
    private final GradientFilter filter;

    private final SystemKCWrapper systemConfig;
    private final GradientKCWrapper gradientConfig;
    private final Random rand;

    //TODO Alex - maybe tree map - with utility comparator?
    private final Map<Identifier, GradientContainer> view;

    public GradientView(SystemKCWrapper systemConfig, GradientKCWrapper gradientConfig, Identifier overlayId,
            String logPrefix, Comparator utilityComparator, GradientFilter filter) {
        this.systemConfig = systemConfig;
        this.gradientConfig = gradientConfig;
        this.logPrefix = logPrefix;
        this.utilityComp = new WrapperComparator<>(utilityComparator);
        this.ageComparator = new InvertedComparator<>(new GradientContainerAgeComparator()); //we want old ages in the begining
        this.filter = filter;
        this.view = new HashMap<>();
        this.rand = new Random(systemConfig.seed + overlayId.partition(Integer.MAX_VALUE));
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

    public List<GradientContainer> getAllCopy() {
        List<GradientContainer> sortedList = new ArrayList<>(view.values());
        Collections.sort(sortedList, utilityComp);
        List<GradientContainer> copyList = new ArrayList<>();
        for (GradientContainer gc : sortedList) {
            copyList.add(gc.getCopy());
        }
        return copyList;
    }

    public boolean isTop(GradientContainer node) {
        if (view.isEmpty()) {
            return false;
        }
        List<GradientContainer> sortedList = new ArrayList<>(view.values());
        Collections.sort(sortedList, utilityComp);
        return utilityComp.compare(node, sortedList.get(sortedList.size() - 1)) > 0;
    }

    public int rank(GradientContainer node) {
        if (view.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        if (isTop(node)) {
            return 0;
        }

        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Collections.sort(sortedList, utilityComp);

        GradientContainer higher = null;
        int rankAdjust = 1;
        for (GradientContainer gc : sortedList) {
            if (utilityComp.compare(node, gc) >= 0) {
                continue;
            }
            if (gc.rank == Integer.MAX_VALUE) {
                rankAdjust++;
            } else {
                higher = gc;
                break;
            }
        }
        if(higher != null) {
            return higher.rank + rankAdjust;
        } else {
            return node.rank;
        }
    }

    

    public void merge(GradientContainer newSample, GradientContainer selfView) {
        Set<GradientContainer> newSet = new HashSet<GradientContainer>();
        newSet.add(newSample);
        merge(newSet, selfView);
    }

    public void merge(Collection<GradientContainer> newSample, GradientContainer selfView) {
        for (GradientContainer gc : newSample) {
            if (gc.getSource().getId().equals(selfView.getSource().getId())) {
                continue;
            }
            if (!filter.retainOther(selfView.getContent(), gc.getContent())) {
                //The Filter says the new descriptor should not be in the view.
                //If we have an old descriptor in the view, we should remove it.
                view.remove(gc.getSource().getId());
                continue;
            }
            GradientContainer currentGc = view.get(gc.getSource().getId());
            if (currentGc != null) {
                if (currentGc.getAge() > gc.getAge()) {
                    view.put(gc.getSource().getId(), gc);
                }
            } else {
                view.put(gc.getSource().getId(), gc);
            }
        }
        log.debug("{} remove - before shrink:{}", new Object[]{logPrefix, view.values()});
        //Should enable cleaning old descriptors even when view is incomplete
        //Even if this cleanup empties the view, we can wait for Croupier to provide new samples
        //We should only remove descriptors older than a defined threshold so we don't disconnect
//        if (view.size() > configCore.viewSize) {
        for (GradientContainer toRemove : reduceSize(ageComparator, 1)) {
            if (toRemove.getAge() >= gradientConfig.oldThreshold) {
                log.debug("{} remove - old:{}", new Object[]{logPrefix, toRemove});
                view.remove(toRemove.getSource().getId());
            }
        }
//        }
        if (view.size() > gradientConfig.viewSize) {
            GradientPreferenceComparator<GradientContainer> preferenceComparator = new GradientPreferenceComparator<GradientContainer>(selfView, utilityComp);
            int reduceSize = view.size() - gradientConfig.viewSize;
            for (GradientContainer toRemove : reduceSize(preferenceComparator, reduceSize)) {
                log.debug("{} remove - self:{} preference bad:{}", new Object[]{logPrefix, selfView, toRemove});
                view.remove(toRemove.getSource().getId());
            }
        }
        log.debug("{} remove - after shrink:{}", logPrefix, view.values());
    }

    public GradientContainer getShuffleNode(GradientContainer selfCPV) {
        if (view.isEmpty()) {
            return null;
        }

        int shuffleNodeIndex = ProbabilitiesHelper.getSoftMaxVal(view.size(), rand, gradientConfig.softMaxTemp);
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Comparator<GradientContainer> selfPrefferenceComparator = new InvertedComparator<GradientContainer>(new GradientPreferenceComparator<GradientContainer>(selfCPV, utilityComp));
        Collections.sort(sortedList, selfPrefferenceComparator);

        return sortedList.get(shuffleNodeIndex);
    }

    public List<GradientContainer> getExchangeCopy(GradientContainer partnerCPV, int n) {
        Comparator<GradientContainer> partnerPrefferenceComparator = new InvertedComparator<GradientContainer>(new GradientPreferenceComparator<GradientContainer>(partnerCPV, utilityComp));
        List<GradientContainer> sortedList = new ArrayList<>(view.values());
        Collections.sort(sortedList, partnerPrefferenceComparator);
        List<GradientContainer> copyList = new ArrayList<>();
        for (int i = 0; i < n && i < sortedList.size(); i++) {
            copyList.add(sortedList.get(i).getCopy());
        }
        return copyList;
    }

    public void clean(KAddress node) {
        view.remove(node.getId());
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
