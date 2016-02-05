/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.croupier.view;

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import se.sics.ktoolbox.croupier.CroupierKCWrapper;
import se.sics.ktoolbox.croupier.history.ShuffleHistory;
import se.sics.ktoolbox.croupier.util.ProbabilisticHelper;
import se.sics.ktoolbox.util.update.view.View;
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LocalView {

    private final CroupierKCWrapper croupierConfig;
    private final Random rand;
    private final Comparator<CroupierContainer> ageComparator = new Comparator<CroupierContainer>() {
        @Override
        public int compare(CroupierContainer o1, CroupierContainer o2) {
            return Ints.compare(o1.getAge(), o2.getAge());
        }
    };

    private final HashMap<Identifier, CroupierContainer> containers = new HashMap<>();

    public LocalView(CroupierKCWrapper croupierConfig, Random rand) {
        this.croupierConfig = croupierConfig;
        this.rand = rand;
    }

    public boolean isEmpty() {
        return containers.isEmpty();
    }

    public int size() {
        return containers.size();
    }

    public void incrementAges() {
        for (CroupierContainer container : containers.values()) {
            container.incrementAge();
        }
    }

    private void addContainer(CroupierContainer container) {
        containers.put(container.src.getId(), container.copy());
    }

    private void removeContainer(ShuffleHistory history, CroupierContainer container) {
        containers.remove(container.src.getId());
        history.remove(container.getSource());
    }

    public NatAwareAddress selectPeerToShuffleWith(ShuffleHistory history) {
        CroupierContainer selectedEntry;
        List<CroupierContainer> containerList = new ArrayList<>(containers.values());
        switch (croupierConfig.policy) {
            case RANDOM:
                selectedEntry = ProbabilisticHelper.generateRandomSample(rand, containerList, 1).get(0);
                break;
            case TAIL:
            case HEALER:
                if (croupierConfig.softMax) {
                    Collections.sort(containerList, ageComparator);
                    int index = ProbabilisticHelper.softMaxIndex(rand, containerList.size(),
                            croupierConfig.softMaxTemp.get());
                    if (index == -1) {
                        selectedEntry = containerList.get(containerList.size() - 1);
                    } else {
                        selectedEntry = containerList.get(index);
                    }
                } else {
                    selectedEntry = Collections.max(containers.values(), ageComparator);
                }
                break;
            default:
                throw new IllegalArgumentException("Croupier policy:" + croupierConfig.policy + " not implemented");
        }

        /**
         * by not removing a reference to the node I am shuffling with, we break
         * the 'batched random walk' (Cyclon) behaviour. But it's more important
         * to keep the graph connected.
         */
        if (containers.size() > croupierConfig.minViewSize) {
            removeContainer(history, selectedEntry);
        }
        return selectedEntry.getSource();
    }

    public Map<Identifier, CroupierContainer> initiatorSample(ShuffleHistory history, NatAwareAddress shufflePartner) {
        return shuffleSample(history, shufflePartner);
    }

    public Map<Identifier, CroupierContainer> receiverSample(ShuffleHistory history, NatAwareAddress shufflePartner) {
        return shuffleSample(history, shufflePartner);
    }

    private Map<Identifier, CroupierContainer> shuffleSample(ShuffleHistory history, NatAwareAddress shufflePartner) {
        List<CroupierContainer> containerList = new ArrayList<>(containers.values());
        containerList = ProbabilisticHelper.generateRandomSample(rand, containerList, croupierConfig.shuffleSize);
        Map<Identifier, CroupierContainer> descriptors = new HashMap<>();
        for (CroupierContainer container : containerList) {
            history.sendTo(container, shufflePartner);
            descriptors.put(container.getSource().getId(), container.copy());
        }
        return descriptors;
    }

    public void selectToKeep(ShuffleHistory history, NatAwareAddress selfAdr,
            Triplet<NatAwareAddress, View, Boolean> partner,
            Map<Identifier, CroupierContainer> partnerContainers) {

        Map<Identifier, CroupierContainer> sentContainers = history.sentTo(partner.getValue0());
        Map<Identifier, CroupierContainer> newContainerSources = new HashMap<>();
        Map<Identifier, Pair<CroupierContainer, CroupierContainer>> newContainerVersions = new HashMap<>();
        Map<Identifier, CroupierContainer> sameContainer = new HashMap<>();
        Map<Identifier, CroupierContainer> oldContainerVersions = new HashMap<>();

        int selfCounter = 0;
        for (CroupierContainer remoteContainer : partnerContainers.values()) {
            if (remoteContainer.getSource().getId().equals(selfAdr.getId())) {
                selfCounter++;
                continue;
            }
            CroupierContainer localContainer = containers.get(remoteContainer.getSource().getId());
            if (localContainer == null) {
                newContainerSources.put(remoteContainer.getSource().getId(), remoteContainer);
            } else if (sameView(localContainer.getContent(), remoteContainer.getContent())) {
                sameContainer.put(remoteContainer.getSource().getId(), remoteContainer);
            } else if (localContainer.getAge() < remoteContainer.getAge()) {
                oldContainerVersions.put(remoteContainer.getSource().getId(), remoteContainer);
            } else {
                newContainerVersions.put(remoteContainer.getSource().getId(), Pair.with(remoteContainer, localContainer));
            }
        }
        assert selfCounter + newContainerSources.size() + newContainerVersions.size() + sameContainer.size()
                + oldContainerVersions.size() == partnerContainers.size();

        //1. ignore old container versions - we have newer;
        //2. add new target container
        if (partner.getValue2()) {
            CroupierContainer old = containers.get(partner.getValue0().getId());
            if (old != null) {
                if (sameView(old.getContent(), partner.getValue1())) {
                    old.resetAge();
                } else {
                    removeContainer(history, old);
                    addContainer(new CroupierContainer(partner.getValue0(), partner.getValue1()));
                }
            } else {
                addContainer(new CroupierContainer(partner.getValue0(), partner.getValue1()));
            }
        }
        //2. update to new versions
        for (Pair<CroupierContainer, CroupierContainer> containerVersion : newContainerVersions.values()) {
            removeContainer(history, containerVersion.getValue1());
            addContainer(containerVersion.getValue0());
        }
        //3. add new sources while keeping size
        for (CroupierContainer container : newContainerSources.values()) {
            addContainer(container);
        }
        //TODO Alex - update policies
        //depending on policy I should save/delete accordingly
        //custom order to shrink back the list - random
        switch (croupierConfig.policy) {
            case HEALER:
            case TAIL:
            case RANDOM:
                if (containers.size() > croupierConfig.viewSize) {
                    int elementsToRemove = containers.size() - croupierConfig.viewSize;
                    List<CroupierContainer> toRemove = new ArrayList<>(containers.values());
                    toRemove = ProbabilisticHelper.generateRandomSample(rand, toRemove, elementsToRemove);
                    for (CroupierContainer container : toRemove) {
                        removeContainer(history, container);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Croupier policy:" + croupierConfig.policy + " not implemented");
        }
        assert containers.size() == croupierConfig.viewSize;
    }

    private boolean sameView(View v1, View v2) {
        if (v1 instanceof Identifiable && v2 instanceof Identifiable) {
            return ((Identifiable) v1).getId().equals(((Identifiable) v2).getId());
        } else {
            return false;
        }
    }

    public Map<NatAwareAddress, CroupierContainer> publish() {
        Map<NatAwareAddress, CroupierContainer> sample = new HashMap<>();
        for (CroupierContainer container : containers.values()) {
            sample.put(container.getSource(), container.copy());
        }
        return sample;
    }

    public void timedOut(ShuffleHistory history, NatAwareAddress shufflePartner) {
        CroupierContainer container = containers.get(shufflePartner.getId());
        if (container == null) {
            return;
        }
        removeContainer(history, container);
    }
}
