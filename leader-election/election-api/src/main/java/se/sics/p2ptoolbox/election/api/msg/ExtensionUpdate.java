package se.sics.p2ptoolbox.election.api.msg;

import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Collection;
import java.util.List;

/**
 * Information from the leader about the extension
 * of the leadership and the updated nodes.
 *
 * Created by babbar on 2015-06-23.
 */
public class ExtensionUpdate implements KompicsEvent {

    public final Collection<DecoratedAddress> groupMembership;

    public ExtensionUpdate(Collection<DecoratedAddress> groupMembership){
        this.groupMembership = groupMembership;
    }
}
