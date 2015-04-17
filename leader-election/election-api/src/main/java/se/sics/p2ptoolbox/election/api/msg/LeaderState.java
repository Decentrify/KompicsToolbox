package se.sics.p2ptoolbox.election.api.msg;

import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Collection;


/**
 * Wrapper class for the marker events for node becoming or losing leader
 * status.
 */
public class LeaderState {

    /**
     * Node can't find anybody it and after the group accepts it as leader, then
     * it is the leader.
     */
    public static class ElectedAsLeader implements KompicsEvent{

        public final Collection<DecoratedAddress> leaderGroup;

        public ElectedAsLeader(Collection<DecoratedAddress> leaderGroup){
            this.leaderGroup = leaderGroup;
        }
    }

    /**
     * Found a node who should be leader and therefore I should back off.
     */
    public static class TerminateBeingLeader implements KompicsEvent{
    }
    
}
