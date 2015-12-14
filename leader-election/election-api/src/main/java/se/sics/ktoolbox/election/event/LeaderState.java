package se.sics.ktoolbox.election.event;


import java.util.Collection;
import se.sics.ktoolbox.util.identifiable.Identifier;
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

        public final Identifier id;
        public final Collection<KAddress> leaderGroup;

        public ElectedAsLeader(Identifier id, Collection<KAddress> leaderGroup){
            this.id = id;
            this.leaderGroup = leaderGroup;
        }

        @Override
        public Identifier getId() {
            return id;
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
