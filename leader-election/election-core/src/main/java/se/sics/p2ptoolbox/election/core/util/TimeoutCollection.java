package se.sics.p2ptoolbox.election.core.util;


import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

import java.util.UUID;

/**
 *
 * Collection of Timeouts used during the election protocol.
 *
 * Created by babbarshaer on 2015-03-31.
 */
public class TimeoutCollection {
    
    // Common.
    public static class LeaseTimeout extends Timeout {
        
        public LeaseTimeout(ScheduleTimeout request) {
            super(request);
        }
    }
    
    // Election Leader.
    public static class PromiseRoundTimeout extends Timeout{

        public PromiseRoundTimeout(ScheduleTimeout request) {
            super(request);
        }
    }
    
    public static class LeaseCommitResponseTimeout extends Timeout{
        
        public LeaseCommitResponseTimeout(ScheduleTimeout request){
            super(request);
        }
    }
    
    //Election Follower.
    
    public static class AwaitLeaseCommitTimeout extends Timeout {

        public UUID electionRoundId;
        
        public AwaitLeaseCommitTimeout(ScheduleTimeout request, UUID electionRoundId) {
            super(request);
            this.electionRoundId = electionRoundId;
        }
    }
    
    
    
}
