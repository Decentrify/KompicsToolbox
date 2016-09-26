package se.sics.ktoolbox.election.event;

import java.util.List;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Information from the leader about the extension
 * of the leadership and the updated nodes.
 *
 * Created by babbar on 2015-06-23.
 */
public class ExtensionUpdate implements ElectionEvent {

    public final Identifier eventId;
    public final List<KAddress> groupMembership;

    public ExtensionUpdate(Identifier eventId, List<KAddress> groupMembership){
        this.eventId = eventId;
        this.groupMembership = groupMembership;
    }
    
    public ExtensionUpdate(List<KAddress> groupMembership){
        this(BasicIdentifiers.eventId(), groupMembership);
    }

    @Override
    public Identifier getId() {
        return eventId;
    }
}
