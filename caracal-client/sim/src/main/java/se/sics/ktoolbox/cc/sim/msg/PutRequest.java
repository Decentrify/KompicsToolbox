package se.sics.ktoolbox.cc.sim.msg;

import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Set;

/**
 * One way request to the caracal client indicating the
 * address of the peer for a particular service information.
 *
 * Created by babbar on 2015-08-17.
 */
public class PutRequest {

    public DecoratedAddress selfAddress;
    public Set<byte[]> serviceIdentifiers;

    public PutRequest(DecoratedAddress selfAddress, Set<byte[]> serviceIdentifiers){
        this.selfAddress = selfAddress;
        this.serviceIdentifiers = serviceIdentifiers;
    }

}
