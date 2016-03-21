package se.sics.ktoolbox.election.msg;

import se.sics.ktoolbox.election.util.LCPeerView;

import java.security.PublicKey;
import java.util.UUID;
import se.sics.ktoolbox.election.event.ElectionEvent;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Container for the extension request sent by the leader in case it thinks
 * after the lease gets over he is still the leader.
 *
 * Created by babbarshaer on 2015-04-02.
 */
public class ExtensionRequest implements ElectionEvent {

    public final Identifier id;
    public final KAddress leaderAddress;
    public final PublicKey leaderPublicKey;
    public final LCPeerView leaderView;
    public final UUID electionRoundId; 
    
    public ExtensionRequest(Identifier id, KAddress leaderAddress, PublicKey publicKey, LCPeerView leaderView, UUID electionRoundId){
        this.id = id;
        this.leaderAddress = leaderAddress;
        this.leaderPublicKey = publicKey;
        this.leaderView = leaderView;
        this.electionRoundId = electionRoundId;
    }

    @Override
    public Identifier getId() {
        return id;
    }
}
