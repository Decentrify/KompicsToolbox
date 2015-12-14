package se.sics.ktoolbox.election.event;

import java.util.Collection;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Information from the leader about the extension
 * of the leadership and the updated nodes.
 *
 * Created by babbar on 2015-06-23.
 */
public class ExtensionUpdate implements ElectionEvent {

    public final Identifier id;
    public final Collection<KAddress> groupMembership;

    public ExtensionUpdate(Identifier id, Collection<KAddress> groupMembership){
        this.id = id;
        this.groupMembership = groupMembership;
    }

    @Override
    public Identifier getId() {
        return id;
    }
}
