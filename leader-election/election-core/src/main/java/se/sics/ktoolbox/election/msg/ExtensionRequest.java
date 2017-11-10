package se.sics.ktoolbox.election.msg;

import java.security.PublicKey;
import java.util.UUID;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.election.event.ElectionEvent;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Container for the extension request sent by the leader in case it thinks
 * after the lease gets over he is still the leader.
 *
 * Created by babbarshaer on 2015-04-02.
 */
public class ExtensionRequest implements ElectionEvent {

    public final Identifier msgId;
    public final KAddress leaderAddress;
    public final PublicKey leaderPublicKey;
    public final LCPeerView leaderView;
    public final UUID electionRoundId; 
    
    public ExtensionRequest(Identifier msgId, KAddress leaderAddress, PublicKey publicKey, LCPeerView leaderView, UUID electionRoundId){
        this.msgId = msgId;
        this.leaderAddress = leaderAddress;
        this.leaderPublicKey = publicKey;
        this.leaderView = leaderView;
        this.electionRoundId = electionRoundId;
    }
    
    public ExtensionRequest(KAddress leaderAddress, PublicKey publicKey, LCPeerView leaderView, UUID electionRoundId){
        this(BasicIdentifiers.msgId(), leaderAddress, publicKey, leaderView, electionRoundId);
    }

    @Override
    public Identifier getId() {
        return msgId;
    }
}
