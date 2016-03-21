package se.sics.p2ptoolbox.election.example.data;

import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Collection;

/**
 * Event sent by the application to the mocked component informing it about the peers in the system.
 * Created by babbar on 2015-04-01.
 */
public class PeersUpdate implements KompicsEvent{

    public Collection<DecoratedAddress> peers;

    public PeersUpdate(Collection<DecoratedAddress> peers){
        this.peers = peers;
    }
}
