package se.sics.ktoolbox.election.event;

import java.security.PublicKey;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Indication event from the election component,
 * stating the information about the current leader.
 * 
 * <br/>
 *
 * It also provides information regarding the leader group membership. 
 * By looking at the information a node can decide whether he is in the group and therefore based on the lease time
 * can update itself.
 *
 * Created by babbarshaer on 2015-03-27.
 */
public class LeaderUpdate implements ElectionEvent {

    public final Identifier eventId;
    public final PublicKey leaderPublicKey;
    public final KAddress leaderAddress;
    
    public LeaderUpdate(Identifier eventId, PublicKey leaderPublicKey, KAddress leaderAddress){
        this.eventId = eventId;
        this.leaderPublicKey = leaderPublicKey;
        this.leaderAddress = leaderAddress;
    }
    
    public LeaderUpdate(PublicKey leaderPublicKey, KAddress leaderAddress){
        this(BasicIdentifiers.eventId(), leaderPublicKey, leaderAddress);
    }

    @Override
    public Identifier getId() {
        return eventId;
    }
}
