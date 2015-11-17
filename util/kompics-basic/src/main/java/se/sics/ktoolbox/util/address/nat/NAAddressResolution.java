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
package se.sics.ktoolbox.util.address.nat;

import se.sics.ktoolbox.util.address.nat.StrippedNAAddress;
import se.sics.ktoolbox.util.address.nat.CompleteNAAddress;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.network.Address;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.ktoolbox.util.address.resolution.AddressResolution;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NAAddressResolution implements AddressResolution {

    private final static Logger LOG = LoggerFactory.getLogger(AddressResolution.class);
    private String logPrefix = "";

    private static final int CLEANUP_NR = 10;

    //TODO Alex - possibly slow to use rwLock, since we will have quite a few writes.
    private final ConcurrentHashMap<StrippedNAAddress, WeakReference<StrippedNAAddress>> referenceDetector
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<StrippedNAAddress, CompleteNAAddress> directRemoteReferences
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<StrippedNAAddress, CompleteNAAddress> indirectRemoteReferences
            = new ConcurrentHashMap<>();

    //TODO Alex cleanup iterators - if you give up on rwLocks, 
    //remember that the maps might get out of sync, so clean properly
    private Iterator<Map.Entry<StrippedNAAddress, WeakReference<StrippedNAAddress>>> refIt
            = referenceDetector.entrySet().iterator();

    //TODO Alex - NOT implemented yet
    private ConcurrentHashMap<StrippedNAAddress, Pair<CompleteNAAddress, CompleteNAAddress>> nonEIMapping
            = new ConcurrentHashMap<>();

    @Override
    public Address localAddress(Address address) {
        if (address instanceof CompleteNAAddress) {
            return ((CompleteNAAddress) address).strip();
        } else {
            LOG.warn("mixing other address types with NatAware Addresses:Stripped/Complete");
            return address;
        }
    }
    
    /**
     * @param address - src address from header of message directly received
     * from the source
     */
    @Override
    public Address setDirect(Address address) {
        if (!(address instanceof CompleteNAAddress)) {
            throw new RuntimeException("mixing other address types with NatAware Addresses:Stripped/Complete");
        }
        CompleteNAAddress naAddress = (CompleteNAAddress)address;
        /**
         * we follow the use of this address using a weak reference to see when
         * the system still has a reference to it. Be careful not to create a
         * memory leak yourself by keeping this stripped address as a key/value
         * in one of the maps
         */
        StrippedNAAddress systemReference = null;
        WeakReference<StrippedNAAddress> auxSR = referenceDetector.get(naAddress.strip());
        if (auxSR != null) {
            systemReference = auxSR.get();
        }
        if (systemReference == null) {
            systemReference = naAddress.strip().shallowCopy();
            referenceDetector.put(naAddress.strip(), new WeakReference(systemReference));
        }
        directRemoteReferences.put(naAddress.strip(), naAddress);
        indirectRemoteReferences.put(naAddress.strip(), naAddress);
        return systemReference;
    }

    /**
     * @param address - any address advertised through content of msgs
     */
    @Override
    public Address setIndirect(Address address) {
        if (!(address instanceof CompleteNAAddress)) {
            throw new RuntimeException("mixing other address types with NatAware Addresses:Stripped/Complete");
        }
        CompleteNAAddress naAddress = (CompleteNAAddress)address;
        /**
         * we follow the use of this address using a weak reference to see when
         * the system still has a reference to it. Be careful not to create a
         * memory leak yourself by keeping this stripped address as a key/value
         * in one of the maps
         */
        StrippedNAAddress systemReference = null;
        WeakReference<StrippedNAAddress> auxSR = referenceDetector.get(naAddress.strip());
        if (auxSR != null) {
            systemReference = auxSR.get();
        }
        if (systemReference == null) {
            systemReference = naAddress.strip().shallowCopy();
            referenceDetector.put(naAddress.strip(), new WeakReference(systemReference));
        }
        indirectRemoteReferences.put(naAddress.strip(), naAddress);
        return systemReference;
    }

    /**
     * @param address
     * @return first try to resolve from directly observed addresses
     */
    @Override
    public Address resolve(Address address) {
        if (!(address instanceof StrippedNAAddress)) {
            throw new RuntimeException("mixing other address types with NatAware Addresses:Stripped/Complete");
        }
        StrippedNAAddress naAddress = (StrippedNAAddress)address;
        CompleteNAAddress cAdr = directRemoteReferences.get(naAddress);
        if (cAdr == null) {
            cAdr = indirectRemoteReferences.get(naAddress);
        }
        if (cAdr == null) {
            /**
             * theoretically should never be the case. If I have a
             * StrippedNAAddress in the system, I should have a corresponding
             * CompleteNAAddress
             */
            //for the moment, returning no parents CompleteNAAddress
            cAdr = naAddress.complete(new ArrayList<BasicAddress>());
        }
        return cAdr;
    }
    
    @Override
    public void cleanup() {
        int cleanupNr = CLEANUP_NR;
        while (cleanupNr > 0 && refIt.hasNext()) {
            cleanupNr--;
            Map.Entry<StrippedNAAddress, WeakReference<StrippedNAAddress>> adrRef = refIt.next();
            if (adrRef.getValue().get() == null) {
                refIt.remove();
                directRemoteReferences.remove(adrRef.getKey());
                indirectRemoteReferences.remove(adrRef.getKey());
            }
        }
        if (!refIt.hasNext()) {
            refIt = referenceDetector.entrySet().iterator();
        }
    }
}
