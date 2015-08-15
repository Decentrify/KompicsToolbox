package se.sics.ktoolbox.cc.heartbeat.sim;

import se.sics.kompics.Init;

/**
 * Initialization class for the main simulation version of the
 * caracal client.
 *
 * Created by babbar on 2015-08-15.
 */
public class CCSimMainInit extends Init<CCSimMain>{

    public int slotLength;

    public CCSimMainInit (int slotLength){
        this.slotLength = slotLength;
    }
}
