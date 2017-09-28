package se.sics.ktoolbox.election.event;


import java.util.List;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.network.KAddress;


/**
 * Wrapper class for the marker events for node becoming or losing leader
 * status.
 */
public class LeaderState {

    /**
     * Node can't find anybody it and after the group accepts it as leader, then
     * it is the leader.
     */
    public static class ElectedAsLeader implements ElectionEvent {

        public final Identifier eventId;
        public final List<KAddress> leaderGroup;

        public ElectedAsLeader(Identifier eventId, List<KAddress> leaderGroup){
            this.eventId = eventId;
            this.leaderGroup = leaderGroup;
        }
        
        public ElectedAsLeader(List<KAddress> leaderGroup){
            this(BasicIdentifiers.eventId(), leaderGroup);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
    }

    /**
     * Found a node who should be leader and therefore I should back off.
     */
    public static class TerminateBeingLeader implements ElectionEvent {

        @Override
        public Identifier getId() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
}
