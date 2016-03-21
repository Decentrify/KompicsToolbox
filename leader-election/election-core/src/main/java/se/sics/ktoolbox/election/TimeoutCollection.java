package se.sics.ktoolbox.election;

import java.util.UUID;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.ktoolbox.election.event.ElectionEvent;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

/**
 *
 * Collection of Timeouts used during the election protocol.
 *
 * Created by babbarshaer on 2015-03-31.
 */
public class TimeoutCollection {
    
    // Common.
    public static class LeaseTimeout extends Timeout implements ElectionEvent {
        
        public LeaseTimeout(ScheduleTimeout request) {
            super(request);
        }

        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }
    
    // Election Leader.
    public static class PeriodicVoting extends Timeout implements ElectionEvent {

        public PeriodicVoting(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }


    public static class PromiseRoundTimeout extends Timeout implements ElectionEvent {

        public PromiseRoundTimeout(ScheduleTimeout request) {
            super(request);
        }
        
        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }
    
    public static class LeaseCommitResponseTimeout extends Timeout implements ElectionEvent{
        
        public LeaseCommitResponseTimeout(ScheduleTimeout request){
            super(request);
        }
        
        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }
    
    //Election Follower.
    
    public static class AwaitLeaseCommitTimeout extends Timeout implements ElectionEvent {

        public UUID electionRoundId;
        
        public AwaitLeaseCommitTimeout(ScheduleTimeout request, UUID electionRoundId) {
            super(request);
            this.electionRoundId = electionRoundId;
        }
        
        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }
}