package se.sics.ktoolbox.election.msg;



import java.security.PublicKey;
import java.util.UUID;
import se.sics.ktoolbox.election.event.ElectionEvent;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Container class for the information exchanged between the node trying to 
 * assert itself as leader and the nodes in the system that it thinks should be 
 * in the leader group.
 *
 * Created by babbarshaer on 2015-03-29.
 */
public class LeaseCommitUpdated {

    public static class Request implements ElectionEvent {
        
        public final Identifier id;
        public final KAddress leaderAddress;
        public final PublicKey leaderPublicKey;
        public final LCPeerView leaderView;
        public final UUID electionRoundId;
        
        public Request(Identifier id, KAddress leaderAddress, PublicKey publicKey, LCPeerView leaderView, UUID electionRoundId){
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
        
        public Response answer(boolean isCommit) {
            return new Response(id, isCommit, electionRoundId);
        }
    }

    public static class Response implements ElectionEvent {
        
        public final Identifier id;
        public final boolean isCommit;
        public final UUID electionRoundId;
        
        public Response(Identifier id, boolean isCommit, UUID electionRoundId){
            this.id = id;
            this.isCommit = isCommit;
            this.electionRoundId = electionRoundId;
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
}
