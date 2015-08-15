package se.sics.ktoolbox.cc.heartbeat.sim;

import se.sics.kompics.Init;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * Initialization component for the simulation version of the
 * caracal heartbeat component.
 *
 * Created by babbar on 2015-08-15.
 */
public class CCHeartbeatSimInit extends Init<CCHeartbeatSimComp>{

    public final DecoratedAddress selfAddress;
    public final DecoratedAddress caracalClientAddress;

    public CCHeartbeatSimInit(DecoratedAddress selfAddress, DecoratedAddress caracalClientAddress){
        this.selfAddress = selfAddress;
        this.caracalClientAddress = caracalClientAddress;
    }
}
