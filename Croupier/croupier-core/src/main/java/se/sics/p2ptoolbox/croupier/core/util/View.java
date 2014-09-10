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
package se.sics.p2ptoolbox.croupier.core.util;

import se.sics.p2ptoolbox.croupier.api.CroupierSelectionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class View {

    private PeerView selfView;
    private final Random rand;
    private final int viewSize;
    private final List<ViewEntry> entries;
    private final HashMap<VodAddress, ViewEntry> d2e;

    private final Comparator<ViewEntry> comparatorByAge = new Comparator<ViewEntry>() {
        @Override
        public int compare(ViewEntry o1, ViewEntry o2) {
            if (o1.getAge() > o2.getAge()) {
                return 1;
            } else if (o1.getAge() < o2.getAge()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    public View(PeerView selfView, Random rand, int viewSize) {
        this.selfView = selfView;
        this.rand = rand;
        this.viewSize = viewSize;
        this.entries = new ArrayList<ViewEntry>();
        this.d2e = new HashMap<VodAddress, ViewEntry>();
    }

    public void incrementDescriptorAges() {
        for (ViewEntry entry : entries) {
            entry.incrementAge();
        }
    }

    public VodAddress selectPeerToShuffleWith(CroupierSelectionPolicy policy, boolean softmax, double temperature) {
        if (entries.isEmpty()) {
            return null;
        }

        ViewEntry selectedEntry = null;

        if (!softmax || policy == CroupierSelectionPolicy.RANDOM) {
            if (policy == CroupierSelectionPolicy.TAIL) {
                selectedEntry = Collections.max(entries, comparatorByAge);
            } else if (policy == CroupierSelectionPolicy.HEALER) {
                selectedEntry = Collections.max(entries, comparatorByAge);
            } else if (policy == CroupierSelectionPolicy.RANDOM) {
                selectedEntry = entries.get(rand.nextInt(entries.size()));
            } else {
                throw new IllegalArgumentException("Invalid Croupier policy selected:" + policy);
            }
        } else {
            List<ViewEntry> tempEntries = new ArrayList<ViewEntry>(entries);
            if (policy == CroupierSelectionPolicy.TAIL) {
                Collections.sort(tempEntries, comparatorByAge);
            } else if (policy == CroupierSelectionPolicy.HEALER) {
                Collections.sort(tempEntries, comparatorByAge);
            } else {
                throw new IllegalArgumentException("Invalid Croupier policy selected:" + policy);
            }

            double rnd = rand.nextDouble();
            double total = 0.0d;
            double[] values = new double[tempEntries.size()];
            int j = tempEntries.size() + 1;
            for (int i = 0; i < tempEntries.size(); i++) {
                // get inverse of values - lowest have highest value.
                double val = j;
                j--;
                values[i] = Math.exp(val / temperature);
                total += values[i];
            }

            boolean found = false;
            for (int i = 0; i < values.length; i++) {
                if (i != 0) {
                    values[i] += values[i - 1];
                }
                // normalise the probability
                double normalisedReward = values[i] / total;
                if (normalisedReward >= rnd) {
                    selectedEntry = tempEntries.get(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                selectedEntry = tempEntries.get(tempEntries.size() - 1);
            }
        }

        // TODO - by not removing a reference to the node I am shuffling with, we
        // break the 'batched random walk' (Cyclon) behaviour. But it's more important
        // to keep the graph connected.
        if (entries.size() >= viewSize) {
            removeEntry(selectedEntry);
        }

        return selectedEntry.peerView.getAddress();
    }
    
    public List<PeerView> selectToSendAtInitiator(int count, VodAddress destinationPeer) {
        List<ViewEntry> randomEntries = generateRandomSample(count);
        List<PeerView> descriptors = new ArrayList<PeerView>();
        for (ViewEntry cacheEntry : randomEntries) {
            cacheEntry.sentTo(destinationPeer);
            descriptors.add(cacheEntry.peerView);
        }
        return descriptors;
    }
    
    public List<PeerView> selectToSendAtReceiver(int count, VodAddress destinationPeer) {
        List<ViewEntry> randomEntries = generateRandomSample(count);
        List<PeerView> descriptors = new ArrayList<PeerView>();
        for (ViewEntry cacheEntry : randomEntries) {
            cacheEntry.sentTo(destinationPeer);
            descriptors.add(cacheEntry.peerView);
        }
        return descriptors;
    }
    
    public void selectToKeep(VodAddress from, List<PeerView> descriptors) {
        if (from.equals(selfView.getAddress())) {
            return;
        }
        LinkedList<ViewEntry> entriesSentToThisPeer = new LinkedList<ViewEntry>();
        ViewEntry fromEntry = d2e.get(from);
        if (fromEntry != null) {
            entriesSentToThisPeer.add(fromEntry);
        }

        for (ViewEntry cacheEntry : entries) {
            if (cacheEntry.wasSentTo(from)) {
                entriesSentToThisPeer.add(cacheEntry);
            }
        }

        for (PeerView descriptor : descriptors) {
            VodAddress id = descriptor.getAddress();
            if (selfView.getAddress().equals(id)) {
                // do not keep descriptor of self
                continue;
            }
            if (d2e.containsKey(id)) {
                // we already have an entry for this peer. keep the youngest one
                ViewEntry entry = d2e.get(id);
                if (entry.peerView.getAge() > descriptor.getAge()) {
                    // we keep the lowest age descriptor
                    removeEntry(entry);
                    addEntry(new ViewEntry(descriptor));
                }
                continue;
            }
            if (entries.size() < viewSize) {
                // fill an empty slot
                addEntry(new ViewEntry(descriptor));
                continue;
            }
            // replace one slot out of those sent to this peer
            ViewEntry sentEntry = entriesSentToThisPeer.poll();
            if (sentEntry != null) {
                removeEntry(sentEntry);
                addEntry(new ViewEntry(descriptor));
            }
        }
    }
    
    public final List<PeerView> getAll() {
        List<PeerView> descriptors = new ArrayList<PeerView>();
        for (ViewEntry cacheEntry : entries) {
            descriptors.add(cacheEntry.peerView);
        }
        return descriptors;
    }
    
    public final List<VodAddress> getAllAddress() {
        List<VodAddress> all = new ArrayList<VodAddress>();
        for (ViewEntry cacheEntry : entries) {
            all.add(cacheEntry.peerView.getAddress());
        }
        return all;
    }
    
    public final List<VodAddress> getRandomPeers(int count) {
        List<ViewEntry> randomEntries = generateRandomSample(count);
        List<VodAddress> randomPeers = new ArrayList<VodAddress>();

        for (ViewEntry cacheEntry : randomEntries) {
            randomPeers.add(cacheEntry.peerView.getAddress());
        }

        return randomPeers;
    }
    
    private boolean removeEntry(ViewEntry entry) {
        boolean res = entries.remove(entry);
        if (d2e.remove(entry.peerView.getAddress()) == null && res == true) {
            System.err.println("Croupier View corrupted.");
        }
        checkSize();
        return res;
    }
    
    private List<ViewEntry> generateRandomSample(int n) {
        List<ViewEntry> randomEntries;
        if (n >= entries.size()) {
            // return all entries
            randomEntries = new ArrayList<ViewEntry>(entries);
        } else {
            // return count random entries
            randomEntries = new ArrayList<ViewEntry>();
            // Don Knuth, The Art of Computer Programming, Algorithm S(3.4.2)
            int t = 0, m = 0, N = entries.size();
            while (m < n) {
                int x = rand.nextInt(N - t);
                if (x < n - m) {
                    randomEntries.add(entries.get(t));
                    m += 1;
                    t += 1;
                } else {
                    t += 1;
                }
            }
        }
        return randomEntries;
    }

    private void addEntry(ViewEntry entry) {

        // don't add yourself
        if (entry.peerView.getAddress().equals(selfView.getAddress())) {
            return;
        }
        
        if (!entries.contains(entry)) {
            entries.add(entry);
            d2e.put(entry.peerView.getAddress(), entry);
            checkSize();
        } else {
            // replace the entry if it already exists
            removeEntry(entry);
            addEntry(entry);
        }
    }

    public boolean timedOutForShuffle(VodAddress node) {
        ViewEntry entry = d2e.get(node);
        if (entry == null) {
            return false;
        }
        return removeEntry(entry);
    }

    private void checkSize() {
        if (entries.size() != d2e.size()) {
            StringBuilder sb = new StringBuilder("Entries: \n");
            for (ViewEntry d : entries) {
                sb.append(d.toString()).append(", ");
            }
            sb.append(" \n IndexEntries: \n");
            for (VodAddress d : d2e.keySet()) {
                sb.append(d.toString()).append(", ");
            }
            System.err.println(sb.toString());
            throw new RuntimeException("WHD " + entries.size() + " <> " + d2e.size());
        }
    }

    public void initialize(Set<PeerView> insiders) {
        for (PeerView peer : insiders) {
            if (!peer.getAddress().equals(selfView.getAddress())) {
                addEntry(new ViewEntry(peer));
            }
        }
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public void updateSelf(PeerView selfView) {
        this.selfView = selfView;
    }
}
