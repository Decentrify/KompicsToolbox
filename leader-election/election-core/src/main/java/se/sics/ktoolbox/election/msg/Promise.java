package se.sics.ktoolbox.election.msg;

import java.util.UUID;
import se.sics.ktoolbox.election.event.ElectionEvent;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Promise Message Object which is 
 * sent between the nodes in the system as part 
 * of the Leader Election Protocol.
 *
 * Created by babbarshaer on 2015-03-29.
 */
public class Promise {

    
    public static class Request implements ElectionEvent {
        public final Identifier id;
        public final LCPeerView leaderView;
        public final KAddress leaderAddress;
        public final UUID electionRoundId;
        
        public Request(Identifier id, KAddress leaderAddress, LCPeerView leaderView, UUID electionRoundId) {
            this.id = id;
            this.leaderAddress = leaderAddress;
            this.leaderView = leaderView;
            this.electionRoundId = electionRoundId;
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
    
    public static class Response implements ElectionEvent {
        public final Identifier id;
        public final boolean acceptCandidate;
        public final boolean isConverged;
        public final UUID electionRoundId;
        
        public Response(Identifier id, boolean acceptCandidate, boolean isConverged, UUID electionRoundId){
            this.id = id;
            this.acceptCandidate = acceptCandidate;
            this.isConverged = isConverged;
            this.electionRoundId = electionRoundId;
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
}
