package se.sics.ktoolbox.election.event;

import java.util.UUID;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

/**
 * Event from application in the system, indicating
 * updated value of self view.
 *
 * Created by babbarshaer on 2015-03-27.
 */
public class ViewUpdate implements ElectionEvent {
    
    public final Identifier id;
    public final LCPeerView selfPv;
    public final UUID electionRoundId;
    
    public ViewUpdate(Identifier id, UUID electionRoundId, LCPeerView pv){
        this.id = id;
        this.selfPv = pv;
        this.electionRoundId = electionRoundId;
    }
    
    public ViewUpdate(UUID electionRoundId, LCPeerView pv) {
        this(UUIDIdentifier.randomId(), electionRoundId, pv);
    }

    @Override
    public Identifier getId() {
        return id;
    }
}
