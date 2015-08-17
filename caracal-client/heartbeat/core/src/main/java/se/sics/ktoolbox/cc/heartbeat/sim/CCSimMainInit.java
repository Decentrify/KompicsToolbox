package se.sics.ktoolbox.cc.heartbeat.sim;

import se.sics.kompics.Init;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * Initialization class for the main simulation version of the
 * caracal client.
 *
 * Created by babbar on 2015-08-15.
 */
public class CCSimMainInit extends Init<CCSimMain>{

    public int slotLength;
    public DecoratedAddress address;

    public CCSimMainInit (int slotLength, DecoratedAddress selfAddress){

        this.slotLength = slotLength;
        this.address = selfAddress;
    }
}
