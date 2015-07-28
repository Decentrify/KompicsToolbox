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
package se.sics.p2ptoolbox.tgradient.util;

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
import se.sics.p2ptoolbox.gradient.GradientFilter;
import se.sics.p2ptoolbox.gradient.util.GradientContainer;
import se.sics.p2ptoolbox.gradient.util.GradientContainerAgeComparator;
import se.sics.p2ptoolbox.gradient.util.ViewConfig;
import se.sics.p2ptoolbox.tgradient.TreeGradientComp;
import se.sics.p2ptoolbox.tgradient.TreeGradientConfig;
import se.sics.p2ptoolbox.util.InvertedComparator;
import se.sics.p2ptoolbox.util.ProbabilitiesHelper;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class TGParentView {

    private static final Logger log = LoggerFactory.getLogger(TreeGradientComp.class);
    private final Comparator<GradientContainer> ageComparator;
    private final GradientFilter filter;

    private final ViewConfig config;
    private final TreeGradientConfig tGradientConfig;
    private final Random rand;
    private final String logPrefix;

    private final Map<Address, GradientContainer> view;

    public TGParentView(String logPrefix, ViewConfig config, TreeGradientConfig tGradientConfig, GradientFilter filter, Random rand) {
        this.ageComparator = new InvertedComparator<GradientContainer>(new GradientContainerAgeComparator()); //we want old ages in the begining
        this.filter = filter;
        this.view = new HashMap<Address, GradientContainer>();
        this.rand = rand;
        this.logPrefix = logPrefix;
        this.config = config;
        this.tGradientConfig = tGradientConfig;
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

    public void merge(Collection<GradientContainer> newSample, GradientContainer selfView) {
        for (GradientContainer gc : newSample) {
            if (gc.getSource().getBase().equals(selfView.getSource().getBase())) {
                continue;
            }
            if (!filter.retainOther(selfView.getContent(), gc.getContent())) {
                //The Filter says the new descriptor should not be in the view.
                //If we have an old descriptor in the view, we should remove it.
                view.remove(gc.getSource().getBase());
                continue;
            }
            GradientContainer currentGc = view.get(gc.getSource().getBase());
            if (currentGc != null) {
                if (currentGc.getAge() > gc.getAge()) {
                    view.put(gc.getSource().getBase(), gc.getCopy());
                }
            } else {
                view.put(gc.getSource().getBase(), gc.getCopy());
            }
        }
        log.debug("{} remove - before shrink:{}", new Object[]{logPrefix, view.values()});
        //Should enable cleaning old descriptors even when view is incomplete
        //Even if this cleanup empties the view, we can wait for Croupier to provide new samples
        //We should only remove descriptors older than a defined threshold so we don't disconnect
//        if (view.size() > config.viewSize) {
        for (GradientContainer toRemove : reduceSize(ageComparator, 1)) {
            if (toRemove.getAge() >= config.oldThreshold) {
                log.debug("{} remove - old:{}", new Object[]{logPrefix, toRemove});
                view.remove(toRemove.getSource().getBase());
            }
        }
//        }
        if (view.size() > config.viewSize) {
            Comparator<GradientContainer> preferenceDeleteComparator = new InvertedComparator<GradientContainer>(new ParentPreferenceComparator(selfView, tGradientConfig.branching, tGradientConfig.kCenterNodes));
            int reduceSize = view.size() - config.viewSize;
            for (GradientContainer toRemove : reduceSize(preferenceDeleteComparator, reduceSize)) {
                log.debug("{} remove - self:{} preference bad:{}", new Object[]{logPrefix, selfView, toRemove});
                view.remove(toRemove.getSource().getBase());
            }
        }
        log.debug("{} remove - after shrink:{}", logPrefix, view.values());
    }

    public List<GradientContainer> getAllCopy() {
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        List<GradientContainer> copyList = new ArrayList<GradientContainer>();
        for (GradientContainer gc : sortedList) {
            copyList.add(gc.getCopy());
        }
        return copyList;
    }

    public GradientContainer getShuffleNode(GradientContainer selfCPV) {
        if (view.isEmpty()) {
            return null;
        }

        int shuffleNodeIndex = ProbabilitiesHelper.getSoftMaxVal(view.size(), rand, config.exchangeSMTemp);
        List<GradientContainer> sortedList = new ArrayList<GradientContainer>(view.values());
        Comparator<GradientContainer> selfPrefferenceComparator = new ParentPreferenceComparator(selfCPV, tGradientConfig.branching, tGradientConfig.kCenterNodes);
        Collections.sort(sortedList, selfPrefferenceComparator);

        return sortedList.get(shuffleNodeIndex);
    }

    public void clean(DecoratedAddress node) {
        view.remove(node.getBase());
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
